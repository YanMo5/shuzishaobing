package com.campushealth.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "campus.security")
public class CampusSecurityProperties {

    private String adminUsername = "admin";
    private String adminPassword = "root";
    private long tokenExpirationMs = 86400000;
    private int maxLoginAttempts = 5;
    private int lockoutDurationMinutes = 30;

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public long getTokenExpirationMs() {
        return tokenExpirationMs;
    }

    public void setTokenExpirationMs(long tokenExpirationMs) {
        this.tokenExpirationMs = tokenExpirationMs;
    }

    public int getMaxLoginAttempts() {
        return maxLoginAttempts;
    }

    public void setMaxLoginAttempts(int maxLoginAttempts) {
        this.maxLoginAttempts = maxLoginAttempts;
    }

    public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }

    public void setLockoutDurationMinutes(int lockoutDurationMinutes) {
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }
}
