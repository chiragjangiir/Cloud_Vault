package com.cloudvault.web.api;

import com.cloudvault.domain.Share;
import com.cloudvault.domain.User;
import com.cloudvault.service.FileService;
import com.cloudvault.service.ObjectStorageService;
import com.cloudvault.service.RateLimitService;
import com.cloudvault.service.ShareService;
import com.cloudvault.web.CurrentUser;
import com.cloudvault.web.RangeStreamer;
import com.cloudvault.web.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class SharesApiController {

    public record ShareCreateRequest(long fileId, String type, String permission,
                                     String recipient, Instant expiresAt, Integer maxDownloads) {}

    public record ShareDto(long id, long fileId, String fileName, String type, String permission,
                           String recipient, String token, Instant expiresAt, Instant revokedAt,
                           int downloadCount, Instant createdAt, String url) {}

    private final ShareService shareService;
    private final FileService fileService;
    private final ObjectStorageService objectStorage;
    private final CurrentUser currentUser;
    private final RateLimitService rateLimitService;

    public SharesApiController(ShareService shareService, FileService fileService,
                               ObjectStorageService objectStorage, CurrentUser currentUser,
                               RateLimitService rateLimitService) {
        this.shareService = shareService;
        this.fileService = fileService;
        this.objectStorage = objectStorage;
        this.currentUser = currentUser;
        this.rateLimitService = rateLimitService;
    }

    private ShareDto dto(Share s, boolean includeToken) {
        return new ShareDto(s.getId(), s.getFile().getId(), s.getFile().getName(),
                s.getType().name(), s.getPermission().name(),
                s.getRecipient() == null ? null : s.getRecipient().getUsername(),
                includeToken ? s.getToken() : null,
                s.getExpiresAt(), s.getRevokedAt(), s.getDownloadCount(), s.getCreatedAt(),
                s.getType() == Share.Type.PUBLIC_LINK && s.getToken() != null
                        ? shareService.publicUrl(s.getToken()) : null);
    }

    // ------------------------------------------------------------------
    // Authenticated share management
    // ------------------------------------------------------------------

    @PostMapping("/shares")
    public ResponseEntity<ShareDto> create(@RequestBody ShareCreateRequest req,
                                           HttpServletRequest request) {
        User user = currentUser.require();
        rateLimitService.consume("share", String.valueOf(user.getId()), 60, Duration.ofHours(1));
        Share.Type type;
        Share.Permission permission;
        try {
            type = Share.Type.valueOf(req.type() == null ? "PUBLIC_LINK" : req.type());
            permission = Share.Permission.valueOf(req.permission() == null ? "VIEW" : req.permission());
        } catch (IllegalArgumentException e) {
            throw ApiException.unprocessable("type must be PUBLIC_LINK or USER_SHARE; permission must be VIEW, DOWNLOAD or EDIT");
        }
        Instant expires = req.expiresAt();
        if (expires != null && !expires.isAfter(Instant.now())) {
            throw ApiException.unprocessable("expiresAt must be in the future");
        }
        Share share;
        if (type == Share.Type.PUBLIC_LINK) {
            share = shareService.createPublicLink(user, req.fileId(), permission, expires, req.maxDownloads());
        } else {
            if (req.recipient() == null || req.recipient().isBlank()) {
                throw ApiException.unprocessable("recipient is required for USER_SHARE");
            }
            share = shareService.createUserShare(user, req.fileId(), req.recipient(), permission, expires);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(dto(share, true));
    }

    @GetMapping("/shares")
    public List<ShareDto> myShares() {
        User user = currentUser.require();
        return shareService.ownedBy(user).stream().map(s -> dto(s, true)).toList();
    }

    @GetMapping("/shares/incoming")
    public List<ShareDto> incoming() {
        User user = currentUser.require();
        return shareService.sharedWith(user).stream().map(s -> dto(s, false)).toList();
    }

    @DeleteMapping("/shares/{id}")
    public ResponseEntity<Void> revoke(@PathVariable long id) {
        User user = currentUser.require();
        shareService.revoke(user, id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Public share access (token-authorized, server-enforced expiry)
    // ------------------------------------------------------------------

    @GetMapping("/public/shares/{token}")
    public ShareDto publicShare(@PathVariable String token) {
        rateLimitService.consume("share-view", token, 120, Duration.ofMinutes(1));
        Share share = shareService.resolvePublic(token);
        return dto(share, false);
    }

    @GetMapping("/public/shares/{token}/download")
    public void publicDownload(@PathVariable String token,
                               @RequestParam(defaultValue = "false") boolean inline,
                               HttpServletRequest request, HttpServletResponse response) throws IOException {
        rateLimitService.consume("share-download", token, 60, Duration.ofMinutes(1));
        Share share = shareService.resolvePublic(token);
        FileService.Download d = fileService.prepareDownload(null, share.getFile().getId(), token);
        RangeStreamer.write(request, response, d.file().getContentType(), d.file().getName(),
                inline, d.version().getSizeBytes(), d.version().getChecksumSha256(),
                () -> objectStorage.openRead(d.object(), 0, -1));
    }

    /** New version upload through an EDIT share. */
    @PostMapping("/public/shares/{token}/upload")
    public ResponseEntity<FilesApiController.FileDto> publicUpload(@PathVariable String token,
                                                                   HttpServletRequest request) {
        rateLimitService.consume("share-upload", token, 20, Duration.ofMinutes(1));
        Share share = shareService.resolvePublic(token);
        long declared = request.getContentLengthLong();
        if (declared < 0) throw ApiException.unprocessable("Content-Length header is required");
        String contentType = request.getContentType() == null
                ? "application/octet-stream" : request.getContentType();
        User viewer = null;
        try {
            viewer = currentUser.require();
        } catch (ApiException ignored) {
            // anonymous edit via public token is allowed when the share permits EDIT
        }
        java.io.InputStream body;
        try {
            body = request.getInputStream();
        } catch (IOException e) {
            throw ApiException.unprocessable("Could not read request body: " + e.getMessage());
        }
        var updated = fileService.uploadVersionByShare(share,
                viewer != null ? viewer : share.getOwner(), contentType, body, declared);
        return ResponseEntity.ok(FilesApiController.dto(updated));
    }
}
