package com.cloudvault.unit;

import com.cloudvault.service.StorageTierClassifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StorageTierClassifierTest {
    @Test
    void testClassificationByAge() {
        assertEquals(StorageTierClassifier.Tier.HOT, StorageTierClassifier.classify(5, 1024));
        assertEquals(StorageTierClassifier.Tier.WARM, StorageTierClassifier.classify(45, 1024));
        assertEquals(StorageTierClassifier.Tier.COLD, StorageTierClassifier.classify(180, 1024));
        assertEquals(StorageTierClassifier.Tier.ARCHIVE, StorageTierClassifier.classify(400, 1024));
    }
}
