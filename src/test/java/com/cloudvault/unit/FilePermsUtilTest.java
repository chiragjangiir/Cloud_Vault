package com.cloudvault.unit;

import com.cloudvault.service.FilePermsUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FilePermsUtilTest {
    @Test
    void testPosixPermStrings() {
        assertEquals("rwxr-xr-x", FilePermsUtil.toPermissionString(0755));
        assertEquals("rw-r--r--", FilePermsUtil.toPermissionString(0644));
        assertEquals("---------", FilePermsUtil.toPermissionString(0000));
    }

    @Test
    void testParsePerms() {
        assertEquals(0755, FilePermsUtil.fromPermissionString("rwxr-xr-x"));
        assertEquals(0644, FilePermsUtil.fromPermissionString("rw-r--r--"));
    }
}
