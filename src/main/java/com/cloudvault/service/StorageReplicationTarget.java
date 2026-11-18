package com.cloudvault.service;

public record StorageReplicationTarget(String destinationBucket, String destinationRegion, boolean replicateDeleteMarkers) {
    public boolean isValid() {
        return destinationBucket != null && !destinationBucket.isBlank() &&
               destinationRegion != null && !destinationRegion.isBlank();
    }

    public String toArn() {
        return String.format("arn:aws:s3:::%s", destinationBucket);
    }
}
