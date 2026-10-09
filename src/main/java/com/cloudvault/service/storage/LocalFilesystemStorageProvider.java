package com.cloudvault.service.storage;

import com.cloudvault.domain.StorageLocation;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Local filesystem storage provider.
 *
 * <p>Writes are streamed to a temporary file in the target directory and then
 * atomically moved into place, so partially written blobs are never visible.
 * The SHA-256 checksum is computed while streaming.</p>
 *
 * <p>Path safety: storage keys are generated internally (sharded UUID) and
 * {@link #resolve} rejects absolute keys, {@code ..} segments, null bytes and
 * anything that would land outside the location root — even if a key were
 * somehow tampered with.</p>
 */
@Component
public class LocalFilesystemStorageProvider implements StorageProvider {

    private static final int BUFFER = 64 * 1024;

    @Override
    public WriteResult write(StorageLocation location, String storageKey, InputStream in) throws IOException {
        Path target = resolve(location, storageKey);
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".part-" + UUID.randomUUID());
        long size = 0;
        String checksum;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (OutputStream fos = Files.newOutputStream(tmp, StandardOpenOption.CREATE_NEW);
                 OutputStream dos = new DigestOutputStream(fos, digest)) {
                byte[] buf = new byte[BUFFER];
                int n;
                while ((n = in.read(buf)) != -1) {
                    dos.write(buf, 0, n);
                    size += n;
                }
                dos.flush();
            }
            checksum = HexFormat.of().formatHex(digest.digest());
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        } finally {
            Files.deleteIfExists(tmp);
        }
        if (Files.size(target) != size) {
            Files.deleteIfExists(target);
            throw new IOException("Stored size mismatch after write");
        }
        return new WriteResult(size, checksum, target.toAbsolutePath().toString());
    }

    @Override
    public InputStream openRead(StorageLocation location, String storageKey, long offset, long length) throws IOException {
        Path target = resolve(location, storageKey);
        if (!Files.isRegularFile(target) || !Files.isReadable(target)) {
            throw new NoSuchFileException("Object not available: " + storageKey);
        }
        var channel = Files.newByteChannel(target, StandardOpenOption.READ);
        if (offset > 0) channel.position(offset);
        InputStream src = java.nio.channels.Channels.newInputStream(channel);
        if (length < 0) {
            return src;
        }
        // Bound the stream to exactly `length` bytes for range requests.
        return new java.io.FilterInputStream(src) {
            private long remaining = length;

            @Override
            public int read() throws IOException {
                if (remaining <= 0) return -1;
                int b = super.read();
                if (b != -1) remaining--;
                return b;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (remaining <= 0) return -1;
                int n = super.read(b, off, (int) Math.min(len, remaining));
                if (n > 0) remaining -= n;
                return n;
            }
        };
    }

    @Override
    public void delete(StorageLocation location, String storageKey) throws IOException {
        Files.deleteIfExists(resolve(location, storageKey));
    }

    @Override
    public boolean exists(StorageLocation location, String storageKey) {
        try {
            return Files.isRegularFile(resolve(location, storageKey));
        } catch (InvalidPathException e) {
            return false;
        }
    }

    @Override
    public long size(StorageLocation location, String storageKey) throws IOException {
        return Files.size(resolve(location, storageKey));
    }

    @Override
    public Path resolve(StorageLocation location, String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Empty storage key");
        }
        if (storageKey.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path root = Paths.get(location.getRootPath()).toAbsolutePath().normalize();
        Path resolved;
        if (storageKey.startsWith("/") || storageKey.startsWith("\\")) {
            throw new IllegalArgumentException("Absolute storage keys are not allowed");
        }
        resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Storage key escapes location root");
        }
        return resolved;
    }

    @Override
    public String newStorageKey() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "objects/" + uuid.substring(0, 2) + "/" + uuid.substring(2, 4) + "/" + uuid + ".blob";
    }
}
