package com.knowledgevault.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent user account — stored separately from the game Player profile.
 * Passwords are NEVER stored in plain text; only the BCrypt hash is kept.
 */
public class UserAccount {

    private String id;

    @NotBlank
    @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username may only contain letters, digits, _ or -")
    private String username;

    /** BCrypt hash — never expose via API responses */
    private String passwordHash;

    private Set<String> roles; // e.g. {"ROLE_USER"}, {"ROLE_ADMIN"}

    private boolean enabled;
    private boolean accountLocked;

    private int failedLoginAttempts;
    private Instant lockoutUntil;

    private Instant createdAt;
    private Instant lastLoginAt;
    private String lastLoginIp;

    // Linked game profile
    private String playerId;

    public UserAccount() {
        this.id = UUID.randomUUID().toString();
        this.roles = Set.of("ROLE_USER");
        this.enabled = true;
        this.accountLocked = false;
        this.failedLoginAttempts = 0;
        this.createdAt = Instant.now();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isAccountLocked() { return accountLocked; }
    public void setAccountLocked(boolean accountLocked) { this.accountLocked = accountLocked; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public Instant getLockoutUntil() { return lockoutUntil; }
    public void setLockoutUntil(Instant lockoutUntil) { this.lockoutUntil = lockoutUntil; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    /** Check if the lockout window has expired */
    public boolean isCurrentlyLocked() {
        if (!accountLocked) return false;
        if (lockoutUntil == null) return true;
        return Instant.now().isBefore(lockoutUntil);
    }
}
