package com.knowledgevault.model.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTOs for authentication endpoints.
 * Validation annotations enforce constraints BEFORE any business logic runs.
 */
public class AuthDTOs {

    // ── Register ──────────────────────────────────────────────────────────────

    public static class RegisterRequest {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 20, message = "Username must be 3–20 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username may only contain letters, digits, _ or -")
        private String username;

        /**
         * Password policy:
         *  - 12–72 characters (BCrypt silently truncates beyond 72)
         *  - At least one uppercase, one lowercase, one digit, one special char
         */
        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 72, message = "Password must be 12–72 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
            message = "Password must contain uppercase, lowercase, digit and special character"
        )
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public static class LoginRequest {
        @NotBlank private String username;
        @NotBlank private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    // ── Token Response ────────────────────────────────────────────────────────

    public static class TokenResponse {
        private final String accessToken;
        private final String refreshToken;
        private final long accessExpiresIn;
        private final String tokenType = "Bearer";

        public TokenResponse(String accessToken, String refreshToken, long accessExpiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.accessExpiresIn = accessExpiresIn;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public long getAccessExpiresIn() { return accessExpiresIn; }
        public String getTokenType() { return tokenType; }
    }

    // ── Refresh Request ───────────────────────────────────────────────────────

    public static class RefreshRequest {
        @NotBlank private String refreshToken;
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    // ── Safe User View (no password hash) ────────────────────────────────────

    public static class UserView {
        private final String id;
        private final String username;
        private final String playerId;

        public UserView(String id, String username, String playerId) {
            this.id = id;
            this.username = username;
            this.playerId = playerId;
        }

        public String getId() { return id; }
        public String getUsername() { return username; }
        public String getPlayerId() { return playerId; }
    }
}
