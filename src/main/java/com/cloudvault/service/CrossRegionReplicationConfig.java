package com.cloudvault.service;

public record CrossRegionReplicationConfig(String roleArn, String destinationBucketArn, String destinationStorageClass) {
    public boolean isValid() {
        return roleArn != null && !roleArn.isBlank() &&
               destinationBucketArn != null && !destinationBucketArn.isBlank();
    }
}
