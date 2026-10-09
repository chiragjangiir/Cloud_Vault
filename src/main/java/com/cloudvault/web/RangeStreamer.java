package com.cloudvault.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Streams stored objects with HTTP Range support (206 partial content),
 * ETag/304 revalidation and a real checksum header. The stream is opened
 * before headers are committed so storage failures surface as proper errors.
 */
public final class RangeStreamer {

    private static final int BUFFER = 64 * 1024;

    private RangeStreamer() {}

    public record Range(long start, long end) {
        public long length() { return end - start + 1; }
    }

    public static void write(HttpServletRequest request, HttpServletResponse response,
                             String contentType, String fileName, boolean inline,
                             long totalSize, String sha256,
                             Supplier<InputStream> streamFactory) throws IOException {
        String etag = "\"" + (sha256 == null ? Long.toString(totalSize) : sha256) + "\"";
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null && ifNoneMatch.contains(etag)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            response.setHeader("ETag", etag);
            return;
        }

        Range range = parseRange(request.getHeader("Range"), totalSize);

        try (InputStream in = streamFactory.get()) { // open first: storage errors become 503/JSON
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("ETag", etag);
            if (sha256 != null) response.setHeader("X-Checksum-SHA256", sha256);
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Content-Disposition", contentDisposition(fileName, inline));

            OutputStream out = response.getOutputStream();
            if (range == null) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType(contentType);
                response.setContentLengthLong(totalSize);
                copy(in, out, totalSize);
            } else {
                if (range.start() > 0) in.skipNBytes(range.start());
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setContentType(contentType);
                response.setHeader("Content-Range",
                        "bytes " + range.start() + "-" + range.end() + "/" + totalSize);
                response.setContentLengthLong(range.length());
                copy(in, out, range.length());
            }
            out.flush();
        }
    }

    /** Parses a single-range Range header; unsupported forms → null (full body). */
    public static Range parseRange(String header, long totalSize) {
        if (header == null || totalSize <= 0) return null;
        String h = header.trim().toLowerCase(Locale.ROOT);
        if (!h.startsWith("bytes=")) return null;
        String spec = h.substring(6).trim();
        if (spec.contains(",")) return null; // multi-range not supported → full body
        int dash = spec.indexOf('-');
        if (dash < 0) return null;
        String startStr = spec.substring(0, dash).trim();
        String endStr = spec.substring(dash + 1).trim();
        try {
            if (startStr.isEmpty()) {
                // suffix range: last N bytes
                if (endStr.isEmpty()) return null;
                long suffix = Long.parseLong(endStr);
                if (suffix <= 0) return null;
                long start = Math.max(0, totalSize - suffix);
                return new Range(start, totalSize - 1);
            }
            long start = Long.parseLong(startStr);
            long end = endStr.isEmpty() ? totalSize - 1 : Long.parseLong(endStr);
            if (start > end || start >= totalSize) return null;
            return new Range(start, Math.min(end, totalSize - 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void copy(InputStream in, OutputStream out, long length) throws IOException {
        byte[] buf = new byte[BUFFER];
        long remaining = length;
        while (remaining > 0) {
            int n = in.read(buf, 0, (int) Math.min(buf.length, remaining));
            if (n < 0) break;
            out.write(buf, 0, n);
            remaining -= n;
        }
    }

    private static String contentDisposition(String fileName, boolean inline) {
        String safe = fileName == null ? "file" : fileName.replaceAll("[\\r\\n\"\\\\]", "_");
        String encoded = URLEncoder.encode(safe, StandardCharsets.UTF_8).replace("+", "%20");
        return (inline ? "inline" : "attachment") + "; filename=\"" + safe + "\"; filename*=UTF-8''" + encoded;
    }
}
