package com.cloudvault.service;

import com.cloudvault.domain.FileEntry;
import com.cloudvault.domain.Folder;
import com.cloudvault.domain.User;
import com.cloudvault.repository.FileEntryRepository;
import com.cloudvault.repository.FolderRepository;
import com.cloudvault.web.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Database-backed folder hierarchy with breadcrumbs, nested folders, cycle
 * prevention and trash/restore for whole subtrees.
 */
@Service
public class FolderService {

    private final FolderRepository folders;
    private final FileEntryRepository files;
    private final com.cloudvault.config.AppProperties props;

    public FolderService(FolderRepository folders, FileEntryRepository files,
                         com.cloudvault.config.AppProperties props) {
        this.folders = folders;
        this.files = files;
        this.props = props;
    }

    /** Sanitizes a folder name: rejects traversal, separators, control chars. */
    public static String sanitizeName(String raw) {
        if (raw == null) throw ApiException.unprocessable("Name is required");
        String name = raw.trim();
        if (name.isEmpty()) throw ApiException.unprocessable("Name must not be empty");
        if (name.length() > 255) throw ApiException.unprocessable("Name must be at most 255 characters");
        if (name.equals(".") || name.equals("..")) throw ApiException.unprocessable("Invalid name");
        if (name.contains("/") || name.contains("\\") || name.indexOf('\0') >= 0) {
            throw ApiException.unprocessable("Name contains illegal characters");
        }
        for (char c : name.toCharArray()) {
            if (Character.isISOControl(c)) throw ApiException.unprocessable("Name contains illegal characters");
        }
        return name;
    }

    /** Returns the user's live root folder, creating it when missing. */
    @Transactional
    public Folder ensureRoot(User user) {
        return folders.findLiveRoots(user.getId()).stream().findFirst().orElseGet(() -> {
            Folder root = new Folder();
            root.setOwner(user);
            root.setName("Home");
            root.setParent(null);
            return folders.save(root);
        });
    }

    /** All live folders for the account (used by move selectors). */
    @Transactional(readOnly = true)
    public List<Folder> allLive(User user) {
        ensureRoot(user);
        return folders.findByOwnerIdAndDeletedAtIsNullOrderByNameAsc(user.getId());
    }

    @Transactional(readOnly = true)
    public Folder resolveLive(User user, Long folderId) {
        if (folderId == null) return ensureRoot(user);
        Folder folder = folders.findLiveById(user.getId(), folderId)
                .orElseThrow(() -> ApiException.notFound("Folder not found"));
        return folder;
    }

    @Transactional(readOnly = true)
    public Folder getOwned(User user, long folderId) {
        Folder folder = folders.findById(folderId)
                .orElseThrow(() -> ApiException.notFound("Folder not found"));
        if (!folder.getOwner().getId().equals(user.getId())) {
            throw ApiException.notFound("Folder not found"); // no existence leak
        }
        return folder;
    }

    /** Breadcrumb trail root → leaf. */
    @Transactional(readOnly = true)
    public List<Folder> breadcrumbs(User user, Folder leaf) {
        List<Long> ids = folders.findAncestorIds(leaf.getId());
        List<Folder> all = folders.findAllById(ids);
        all.sort(Comparator.comparing(Folder::getId));
        return all;
    }

    @Transactional
    public Folder create(User user, Long parentId, String rawName) {
        String name = sanitizeName(rawName);
        Folder parent = parentId == null ? ensureRoot(user) : getLiveOwned(user, parentId);
        assertNoLiveNameConflict(user, parent, name);
        Folder folder = new Folder();
        folder.setOwner(user);
        folder.setParent(parent);
        folder.setName(name);
        return folders.save(folder);
    }

    @Transactional
    public Folder rename(User user, long folderId, String rawName) {
        String name = sanitizeName(rawName);
        Folder folder = getLiveOwned(user, folderId);
        assertNoLiveNameConflict(user, folder.getParent(), name);
        folder.setName(name);
        folder.touch();
        return folders.save(folder);
    }

    @Transactional
    public Folder move(User user, long folderId, Long targetParentId) {
        Folder folder = getLiveOwned(user, folderId);
        if (folder.getParent() == null) {
            throw ApiException.conflict("The root folder cannot be moved");
        }
        Folder target = targetParentId == null ? ensureRoot(user) : getLiveOwned(user, targetParentId);
        if (target.getId().equals(folder.getId())) {
            throw ApiException.conflict("A folder cannot be moved into itself");
        }
        // Cycle prevention: target must not live inside the moved subtree.
        Set<Long> subtree = new HashSet<>(folders.findSubtreeIds(folder.getId()));
        if (subtree.contains(target.getId())) {
            throw ApiException.conflict("A folder cannot be moved into its own subfolder");
        }
        if (!target.getOwner().getId().equals(user.getId())) {
            throw ApiException.forbidden("Cannot move folders between accounts");
        }
        assertNoLiveNameConflict(user, target, folder.getName());
        folder.setParent(target);
        folder.touch();
        return folders.save(folder);
    }

    /**
     * Moves the folder subtree to trash (soft delete) with retention deadline.
     * The physical objects are untouched — only database state changes.
     */
    @Transactional
    public int trash(User user, long folderId) {
        Folder folder = getLiveOwned(user, folderId);
        if (folder.getParent() == null) {
            throw ApiException.conflict("The root folder cannot be deleted");
        }
        List<Long> subtree = folders.findSubtreeIds(folder.getId());
        Instant now = Instant.now();
        Instant retention = now.plusSeconds(props.getTrashRetentionDays() * 86400L);
        int movedFiles = files.markSubtreeTrashed(subtree, now, retention);
        folders.markTrashed(subtree, now, retention);
        return movedFiles + subtree.size();
    }

    /**
     * Restores a trashed folder subtree, provided its parent chain is live.
     * Individually trashed files inside are left in the trash.
     */
    @Transactional
    public int restore(User user, long folderId) {
        Folder folder = folders.findById(folderId)
                .orElseThrow(() -> ApiException.notFound("Folder not found"));
        if (!folder.getOwner().getId().equals(user.getId())) throw ApiException.notFound("Folder not found");
        if (!folder.isTrashed()) throw ApiException.conflict("Folder is not in the trash");
        if (folder.getParent() != null && folder.getParent().isTrashed()) {
            throw ApiException.conflict("Restore the parent folder first");
        }
        List<Long> subtree = folders.findSubtreeIds(folder.getId());
        int restoredFiles = files.markSubtreeActive(subtree);
        folders.clearTrashed(subtree);
        return restoredFiles + subtree.size();
    }

    /** Permanently deletes a trashed folder subtree (caller purges file objects first). */
    @Transactional
    public List<FileEntry> filesInSubtree(User user, long folderId) {
        Folder folder = folders.findById(folderId)
                .orElseThrow(() -> ApiException.notFound("Folder not found"));
        if (!folder.getOwner().getId().equals(user.getId())) throw ApiException.notFound("Folder not found");
        List<Long> subtree = folders.findSubtreeIds(folder.getId());
        return files.findByFolderIdIn(subtree);
    }

    @Transactional
    public void deleteFolderRows(List<Long> subtreeIds) {
        // Child files rows are deleted by the caller; delete folders deepest-first.
        List<Folder> all = folders.findAllById(subtreeIds);
        all.sort(Comparator.comparing(Folder::getId).reversed());
        folders.deleteAll(all);
    }

    private Folder getLiveOwned(User user, long folderId) {
        return folders.findLiveById(user.getId(), folderId)
                .orElseThrow(() -> ApiException.notFound("Folder not found"));
    }

    private void assertNoLiveNameConflict(User user, Folder parent, String name) {
        Long parentId = parent == null ? null : parent.getId();
        List<Folder> siblings = parentId == null
                ? folders.findLiveRoots(user.getId())
                : folders.findLiveChildren(user.getId(), parentId);
        boolean conflict = siblings.stream().anyMatch(f -> f.getName().equalsIgnoreCase(name));
        if (conflict) {
            throw ApiException.conflict("An item with that name already exists here");
        }
        boolean fileConflict = (parentId == null ? List.<FileEntry>of() : files.findLiveInFolder(user.getId(), parentId))
                .stream().anyMatch(f -> f.getName().equalsIgnoreCase(name));
        if (fileConflict) {
            throw ApiException.conflict("An item with that name already exists here");
        }
    }

    /** Full contents for the file browser. */
    @Transactional(readOnly = true)
    public BrowserView browse(User user, Long folderId) {
        Folder current = folderId == null ? ensureRoot(user) : getLiveOwned(user, folderId);
        List<Folder> crumbs = breadcrumbs(user, current);
        List<Folder> childFolders = folders.findLiveChildren(user.getId(), current.getId());
        List<FileEntry> liveFiles = files.findLiveInFolder(user.getId(), current.getId());
        return new BrowserView(current, crumbs, childFolders, liveFiles);
    }

    public record BrowserView(Folder current, List<Folder> breadcrumbs, List<Folder> folders, List<FileEntry> files) {}

    @Transactional(readOnly = true)
    public List<Folder> trashedRoots(User user) {
        return folders.findTrashedRoots(user.getId());
    }

    /** Names of live folders at the same level (children of {@code parent}). */
    @Transactional(readOnly = true)
    public List<String> liveSiblingNames(User user, Folder parent) {
        if (parent == null || parent.getId() == null) return List.of();
        return folders.findLiveChildren(user.getId(), parent.getId())
                .stream().map(Folder::getName).toList();
    }

    /** Returns the folder when it is live and owned, else null. */
    @Transactional(readOnly = true)
    public Folder resolveLiveOrNull(User user, long folderId) {
        return folders.findLiveById(user.getId(), folderId).orElse(null);
    }
}
