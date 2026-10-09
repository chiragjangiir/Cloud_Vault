package com.cloudvault.web.api;

import com.cloudvault.domain.Folder;
import com.cloudvault.domain.User;
import com.cloudvault.service.FolderService;
import com.cloudvault.web.CurrentUser;
import com.cloudvault.web.error.ApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/folders")
public class FoldersApiController {

    public record FolderRequest(Long parentId, String name) {}

    private final FolderService folderService;
    private final CurrentUser currentUser;

    public FoldersApiController(FolderService folderService, CurrentUser currentUser) {
        this.folderService = folderService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<FilesApiController.FolderDto> create(@RequestBody FolderRequest req) {
        User user = currentUser.require();
        Folder folder = folderService.create(user, req.parentId(), req.name());
        return ResponseEntity.status(201).body(FilesApiController.folderDto(folder));
    }

    @PatchMapping("/{id}")
    public FilesApiController.FolderDto rename(@PathVariable long id, @RequestBody FolderRequest req) {
        User user = currentUser.require();
        return FilesApiController.folderDto(folderService.rename(user, id, req.name()));
    }

    @PostMapping("/{id}/move")
    public FilesApiController.FolderDto move(@PathVariable long id, @RequestBody Map<String, Long> body) {
        User user = currentUser.require();
        return FilesApiController.folderDto(folderService.move(user, id, body.get("targetParentId")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> trash(@PathVariable long id) {
        User user = currentUser.require();
        folderService.trash(user, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable long id) {
        User user = currentUser.require();
        folderService.restore(user, id);
        return ResponseEntity.noContent().build();
    }
}
