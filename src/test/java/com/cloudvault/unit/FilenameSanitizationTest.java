package com.cloudvault.unit;

import com.cloudvault.service.FolderService;
import com.cloudvault.web.error.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Filename sanitization — the first line of defence against path traversal
 * (§8: ../, absolute paths, separators, NUL bytes, control characters).
 */
class FilenameSanitizationTest {

    @Test
    void acceptsOrdinaryNamesAndTrimsWhitespace() {
        assertEquals("Documents", FolderService.sanitizeName("Documents"));
        assertEquals("photo (1).jpg", FolderService.sanitizeName("  photo (1).jpg  "));
        assertEquals("Ünïcode-файл-123.bin", FolderService.sanitizeName("Ünïcode-файл-123.bin"));
        assertEquals("a".repeat(255), FolderService.sanitizeName("a".repeat(255)));
    }

    @Test
    void rejectsTraversalAndSeparatorAttacks() {
        String[] attacks = {
                "../evil.txt",
                "..\\evil.txt",
                "../../etc/passwd",
                "/etc/passwd",
                "a/b.txt",
                "nested\\folder\\x.txt",
                "..",
                ".",
                "....//x",
                "bad\0name",
                "line\nbreak",
                "tab\tname"
        };
        for (String attack : attacks) {
            ApiException ex = assertThrows(ApiException.class,
                    () -> FolderService.sanitizeName(attack),
                    "must reject: " + attack.replace("\0", "\\0"));
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatus(),
                    "wrong status for: " + attack.replace("\0", "\\0"));
        }
    }

    @Test
    void rejectsNullEmptyAndOversized() {
        assertThrows(ApiException.class, () -> FolderService.sanitizeName(null));
        assertThrows(ApiException.class, () -> FolderService.sanitizeName(""));
        assertThrows(ApiException.class, () -> FolderService.sanitizeName("    "));
        assertThrows(ApiException.class, () -> FolderService.sanitizeName("a".repeat(256)));
    }
}
