package com.cloudvault.service;

public class FilePermsUtil {
    public static String toPermissionString(int mode) {
        char[] chars = new char[9];
        chars[0] = (mode & 0400) != 0 ? 'r' : '-';
        chars[1] = (mode & 0200) != 0 ? 'w' : '-';
        chars[2] = (mode & 0100) != 0 ? 'x' : '-';
        chars[3] = (mode & 0040) != 0 ? 'r' : '-';
        chars[4] = (mode & 0020) != 0 ? 'w' : '-';
        chars[5] = (mode & 0010) != 0 ? 'x' : '-';
        chars[6] = (mode & 0004) != 0 ? 'r' : '-';
        chars[7] = (mode & 0002) != 0 ? 'w' : '-';
        chars[8] = (mode & 0001) != 0 ? 'x' : '-';
        return new String(chars);
    }

    public static int fromPermissionString(String perms) {
        if (perms == null || perms.length() != 9) return 0;
        int mode = 0;
        if (perms.charAt(0) == 'r') mode |= 0400;
        if (perms.charAt(1) == 'w') mode |= 0200;
        if (perms.charAt(2) == 'x') mode |= 0100;
        if (perms.charAt(3) == 'r') mode |= 0040;
        if (perms.charAt(4) == 'w') mode |= 0020;
        if (perms.charAt(5) == 'x') mode |= 0010;
        if (perms.charAt(6) == 'r') mode |= 0004;
        if (perms.charAt(7) == 'w') mode |= 0002;
        if (perms.charAt(8) == 'x') mode |= 0001;
        return mode;
    }
}
