package com.cloudvault.service;

import java.util.Set;

public final class FilenameCollisionResolver {
    private FilenameCollisionResolver() {}

    public static String resolveCollision(String baseName, Set<String> existingNames) {
        if (!existingNames.contains(baseName)) {
            return baseName;
        }
        int dot = baseName.lastIndexOf('.');
        String namePart = dot > 0 ? baseName.substring(0, dot) : baseName;
        String extPart = dot > 0 ? baseName.substring(dot) : "";

        int counter = 1;
        while (true) {
            String candidate = namePart + " (" + counter + ")" + extPart;
            if (!existingNames.contains(candidate)) {
                return candidate;
            }
            counter++;
        }
    }
}
