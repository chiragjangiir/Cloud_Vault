package com.cloudvault.web.api;

import com.cloudvault.domain.FileEntry;
import com.cloudvault.domain.FileVersion;
import com.cloudvault.domain.Folder;
import com.cloudvault.domain.Share;
import com.cloudvault.domain.User;
import com.cloudvault.service.FileService;
import com.cloudvault.service.FolderService;
import com.cloudvault.service.ObjectStorageService;
import com.cloudvault.service.RateLimitService;
import com.cloudvault.web.CurrentUser;
import com.cloudvault.web.RangeStreamer;
import com.cloudvault.web.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
public class FilesApiController {

    public record FileDto(long id, String name, long size, String contentType, String checksum,
                          long folderId, Instant createdAt, Instant updatedAt,
                          int versionCount, String status, Instant retentionUntil) {}

    public record FolderDto(long id, String name, Long parentId) {}

    public record BrowseDto(FolderDto current, List<FolderDto> breadcrumbs,
                            List<FolderDto> folders, List<FileDto> files) {}

    private final FileService fileService;
    private final FolderService folderService;
    private final CurrentUser currentUser;
    private final RateLimitService rateLimitService;
    private final ObjectStorageService objectStorage;

    public FilesApiController(FileService fileService, FolderService folderService,
                              CurrentUser currentUser, RateLimitService rateLimitService,
                              ObjectStorageService objectStorage) {
        this.fileService = fileService;
        this.folderService = folderService;
        this.currentUser = currentUser;
        this.rateLimitService = rateLimitService;
        this.objectStorage = objectStorage;
    }

    static FileDto dto(FileEntry f) {
        return new FileDto(f.getId(), f.getName(), f.getSizeBytes(), f.getContentType(),
                f.getChecksumSha256(), f.getFolder() == null ? -1 : f.getFolder().getId(),
                f.getCreatedAt(), f.getUpdatedAt(), f.getVersionCount(),
                f.getStatus().name(), f.getRetentionUntil());
    }

    static FolderDto folderDto(Folder f) {
        return new FolderDto(f.getId(), f.getName(), f.getParent() == null ? null : f.getParent().getId());
    }

    // ------------------------------------------------------------------
    // Upload (real streaming: request body is consumed once, never buffered)
    // ------------------------------------------------------------------

    @PostMapping(consumes = {"application/octet-stream", "video/*", "image/*", "audio/*", "text/*",
            "application/pdf", "*/*"})
    public ResponseEntity<FileDto> uploadRaw(@RequestParam(required = false) Long folderId,
                                             @RequestParam(required = false) String name,
                                             HttpServletRequest request) {
        User user = currentUser.require();
        rateLimitService.consume("upload", String.valueOf(user.getId()), 120, Duration.ofMinutes(1));
        String fileName = name != null ? name : request.getHeader("X-File-Name");
        if (fileName == null || fileName.isBlank()) {
            throw ApiException.unprocessable("File name is required (query param 'name' or header X-File-Name)");
        }
        long declared = request.getContentLengthLong();
        if (declared < 0) {
            throw ApiException.unprocessable("Content-Length header is required for streaming uploads");
        }
        String contentType = request.getContentType();
        if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";
        java.io.InputStream body;
        try {
            body = request.getInputStream();
        } catch (IOException e) {
            throw ApiException.unprocessable("Could not read request body: " + e.getMessage());
        }
        FileEntry file = fileService.upload(user, folderId, fileName, contentType, body, declared);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto(file));
    }

    /** Multipart fallback (classic form upload). The multipart resolver spools to disk, not RAM. */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<FileDto> uploadMultipart(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(required = false) Long folderId) {
        User user = currentUser.require();
        rateLimitService.consume("upload", String.valueOf(user.getId()), 120, Duration.ofMinutes(1));
        try (InputStream in = file.getInputStream()) {
            String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
            FileEntry stored = fileService.upload(user, folderId, file.getOriginalFilename(),
                    contentType, in, file.getSize());
            return ResponseEntity.status(HttpStatus.CREATED).body(dto(stored));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_READ_FAILED",
                    "Could not read uploaded file: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Browse / list / search
    // ------------------------------------------------------------------

    @GetMapping
    public BrowseDto browse(@RequestParam(required = false) Long folderId) {
        User user = currentUser.require();
        FolderService.BrowserView view = folderService.browse(user, folderId);
        return new BrowseDto(
                folderDto(view.current()),
                view.breadcrumbs().stream().map(FilesApiController::folderDto).toList(),
                view.folders().stream().map(FilesApiController::folderDto).toList(),
                view.files().stream().map(FilesApiController::dto).toList());
    }

    @GetMapping("/recent")
    public List<FileDto> recent() {
        User user = currentUser.require();
        return fileService.recent(user).stream().limit(50).map(FilesApiController::dto).toList();
    }

    @GetMapping("/search")
    public Page<FileDto> search(@RequestParam(required = false) String q,
                                @RequestParam(required = false) String ext,
                                @RequestParam(required = false) String mime,
                                @RequestParam(required = false) Long folderId,
                                @RequestParam(required = false) String from,
                                @RequestParam(required = false) String to,
                                @RequestParam(required = false) Long minSize,
                                @RequestParam(required = false) Long maxSize,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "50") int size) {
        User user = currentUser.require();
        FileService.SearchCriteria criteria = new FileService.SearchCriteria(
                q, ext, mime, folderId, parseInstant(from), parseInstant(to), minSize, maxSize);
        return fileService.search(user, criteria,
                        PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size)),
                                Sort.by(Sort.Direction.DESC, "updatedAt")))
                .map(FilesApiController::dto);
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw ApiException.unprocessable("Invalid timestamp: " + value);
        }
    }

    // ------------------------------------------------------------------
    // Metadata / download
    // ------------------------------------------------------------------

    @GetMapping("/{id}")
    public FileDto get(@PathVariable long id,
                       @RequestParam(required = false) String shareToken) {
        User user = optionalUser();
        FileEntry file = fileService.authorizeView(user, id, shareToken);
        return dto(file);
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable long id,
                         @RequestParam(required = false) String shareToken,
                         @RequestParam(defaultValue = "false") boolean inline,
                         HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = optionalUser();
        FileService.Download d = fileService.prepareDownload(user, id, shareToken);
        RangeStreamer.write(request, response, d.file().getContentType(), d.file().getName(),
                inline, d.version().getSizeBytes(), d.version().getChecksumSha256(),
                () -> objectStorage.openRead(d.object(), 0, -1));
    }

    private User optionalUser() {
        try {
            return currentUser.require();
        } catch (ApiException e) {
            return null; // share-token access may be anonymous
        }
    }

    // ------------------------------------------------------------------
    // Mutations
    // ------------------------------------------------------------------

    @PutMapping("/{id}")
    public FileDto rename(@PathVariable long id, @RequestBody Map<String, String> body) {
        User user = currentUser.require();
        String name = body.get("name");
        if (name == null || name.isBlank()) throw ApiException.unprocessable("name is required");
        return dto(fileService.rename(user, id, name));
    }

    @PostMapping("/{id}/move")
    public FileDto move(@PathVariable long id, @RequestBody Map<String, Long> body) {
        User user = currentUser.require();
        return dto(fileService.move(user, id, body.get("folderId")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> trash(@PathVariable long id) {
        User user = currentUser.require();
        fileService.trash(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable long id) {
        User user = currentUser.require();
        fileService.restore(user, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> purge(@PathVariable long id) {
        User user = currentUser.require();
        fileService.purge(user, id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Versions
    // ------------------------------------------------------------------

    public record VersionDto(int versionNumber, long size, String checksum, String contentType,
                             Instant createdAt, boolean active) {}

    @GetMapping("/{id}/versions")
    public List<VersionDto> versions(@PathVariable long id) {
        User user = currentUser.require();
        FileEntry file = fileService.getOwned(user, id);
        long activeId = file.getActiveVersion() == null ? -1 : file.getActiveVersion().getId();
        return fileService.listVersions(user, id).stream()
                .map(v -> new VersionDto(v.getVersionNumber(), v.getSizeBytes(), v.getChecksumSha256(),
                        v.getContentType(), v.getCreatedAt(), v.getId() == activeId))
                .toList();
    }

    @PostMapping("/{id}/versions/{number}/activate")
    public ResponseEntity<Void> activateVersion(@PathVariable long id, @PathVariable int number) {
        User user = currentUser.require();
        fileService.activateVersion(user, id, number);
        return ResponseEntity.noContent().build();
    }
}
