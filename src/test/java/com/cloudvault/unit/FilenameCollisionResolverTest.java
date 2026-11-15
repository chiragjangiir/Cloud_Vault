package com.cloudvault.unit;

import com.cloudvault.service.FilenameCollisionResolver;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FilenameCollisionResolverTest {
    @Test
    void testResolvesCollisions() {
        Set<String> existing = Set.of("report.pdf", "report (1).pdf");
        assertEquals("report (2).pdf", FilenameCollisionResolver.resolveCollision("report.pdf", existing));
        assertEquals("other.txt", FilenameCollisionResolver.resolveCollision("other.txt", existing));
    }
}
