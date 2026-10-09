package com.cloudvault.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties("app")
public class AppProperties {

    private final Storage storage = new Storage();
    private final Admin admin = new Admin();
    private final RateLimit rateLimit = new RateLimit();
    private String publicUrl = "http://localhost:8080";
    private int trashRetentionDays = 30;
    private int reservationTimeoutMinutes = 30;
    private long jobPollIntervalMs = 2000;
    private long healthCheckIntervalMs = 60000;

    public static class Storage {
        /** Configured real storage root for the auto-registered default location. */
        private String root = "./data/storage";

        public String getRoot() { return root; }
        public void setRoot(String root) { this.root = root; }
    }

    public static class Admin {
        private String username = "";
        private String password = "";

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RateLimit {
        private Map<String, Integer> limits = new LinkedHashMap<>();

        public Map<String, Integer> getLimits() { return limits; }
        public void setLimits(Map<String, Integer> limits) { this.limits = limits; }
    }

    public Storage getStorage() { return storage; }
    public Admin getAdmin() { return admin; }
    public RateLimit getRateLimit() { return rateLimit; }
    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }
    public int getTrashRetentionDays() { return trashRetentionDays; }
    public void setTrashRetentionDays(int trashRetentionDays) { this.trashRetentionDays = trashRetentionDays; }
    public int getReservationTimeoutMinutes() { return reservationTimeoutMinutes; }
    public void setReservationTimeoutMinutes(int reservationTimeoutMinutes) { this.reservationTimeoutMinutes = reservationTimeoutMinutes; }
    public long getJobPollIntervalMs() { return jobPollIntervalMs; }
    public void setJobPollIntervalMs(long jobPollIntervalMs) { this.jobPollIntervalMs = jobPollIntervalMs; }
    public long getHealthCheckIntervalMs() { return healthCheckIntervalMs; }
    public void setHealthCheckIntervalMs(long healthCheckIntervalMs) { this.healthCheckIntervalMs = healthCheckIntervalMs; }
}
