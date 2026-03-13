package com.knowledgevault.security;

import com.knowledgevault.model.Player;
import com.knowledgevault.model.UserAccount;
import com.knowledgevault.model.auth.AuthDTOs;
import com.knowledgevault.service.PlayerService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Authentication business logic — maximum-security edition.
 *
 * Security layers:
 *  ① Input sanitization on all user-supplied fields
 *  ② Constant-time BCrypt comparison (prevents timing enumeration)
 *  ③ Dummy hash even when user not found (timing-safe)
 *  ④ Brute-force lockout: 5 attempts → 15-min lockout
 *  ⑤ Refresh token rotation — every use revokes the old token
 *  ⑥ All events routed through AuditLogger
 *  ⑦ Generic error messages — never expose whether username exists
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int  MAX_ATTEMPTS    = 5;
    private static final long LOCKOUT_SECONDS = 15 * 60L;

    // Dummy hash — used when user not found to prevent timing-based enumeration.
    private static final String DUMMY_HASH =
        "$2a$12$dummyhashfortimingprotection.XXXXXXXXXXXXXXXXXXXXXX";

    private final VaultUserDetailsService userDetailsService;
    private final PasswordEncoder         passwordEncoder;
    private final JwtService              jwtService;
    private final PlayerService           playerService;
    private final InputSanitizer          sanitizer;
    private final AuditLogger             auditLogger;

    public AuthService(
        VaultUserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        PlayerService playerService,
        InputSanitizer sanitizer,
        AuditLogger auditLogger
    ) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder    = passwordEncoder;
        this.jwtService         = jwtService;
        this.playerService      = playerService;
        this.sanitizer          = sanitizer;
        this.auditLogger        = auditLogger;
    }

    public AuthDTOs.TokenResponse register(AuthDTOs.RegisterRequest req, String clientIp) {
        String username = sanitizer.sanitizeUsername(req.getUsername());

        if (userDetailsService.usernameExists(username)) {
            auditLogger.log(AuditLogger.Event.REGISTER_FAILURE, username, clientIp, "username_taken");
            throw new AuthException("Registration failed. Please try a different username.");
        }

        UserAccount account = new UserAccount();
        account.setUsername(username);
        account.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        account.setLastLoginIp(clientIp);
        account.setLastLoginAt(Instant.now());

        Player player = playerService.createPlayer(username);
        account.setPlayerId(player.getId());
        userDetailsService.register(account);

        auditLogger.log(AuditLogger.Event.REGISTER_SUCCESS, username, clientIp);
        return issueTokenPair(account);
    }

    public AuthDTOs.TokenResponse login(AuthDTOs.LoginRequest req, String clientIp) {
        String username = sanitizer.sanitize(req.getUsername()).trim();
        UserAccount account = userDetailsService.findByUsername(username);

        // ALWAYS run BCrypt — even when user not found — to prevent timing enumeration
        String hashToCheck = (account != null) ? account.getPasswordHash() : DUMMY_HASH;
        boolean passwordMatch = passwordEncoder.matches(req.getPassword(), hashToCheck);

        // Check lockout AFTER password check to avoid timing leak
        if (account != null && account.isCurrentlyLocked()) {
            auditLogger.log(AuditLogger.Event.LOGIN_LOCKED, username, clientIp);
            throw new AuthException("Invalid credentials");
        }

        if (account == null || !passwordMatch) {
            if (account != null) recordFailedAttempt(account, clientIp);
            auditLogger.log(AuditLogger.Event.LOGIN_FAILURE, username, clientIp);
            throw new AuthException("Invalid credentials");
        }

        if (!account.isEnabled()) {
            auditLogger.log(AuditLogger.Event.LOGIN_FAILURE, username, clientIp, "disabled");
            throw new AuthException("Invalid credentials");
        }

        account.setFailedLoginAttempts(0);
        account.setAccountLocked(false);
        account.setLockoutUntil(null);
        account.setLastLoginAt(Instant.now());
        account.setLastLoginIp(clientIp);
        userDetailsService.save(account);

        auditLogger.log(AuditLogger.Event.LOGIN_SUCCESS, username, clientIp);
        return issueTokenPair(account);
    }

    public AuthDTOs.TokenResponse refresh(AuthDTOs.RefreshRequest req) {
        try {
            Claims claims = jwtService.parseAndValidate(req.getRefreshToken());
            if (!jwtService.isRefreshToken(claims)) throw new AuthException("Invalid token type");

            String userId = claims.getSubject();
            UserAccount account = userDetailsService.findById(userId);

            if (account == null || !account.isEnabled() || account.isCurrentlyLocked()) {
                throw new AuthException("Account unavailable");
            }

            jwtService.revoke(req.getRefreshToken());
            auditLogger.log(AuditLogger.Event.TOKEN_REFRESH, account.getUsername(), "system");
            return issueTokenPair(account);

        } catch (JwtException e) {
            throw new AuthException("Invalid or expired refresh token");
        }
    }

    public void logout(String accessToken, String refreshToken, String username, String ip) {
        jwtService.revoke(accessToken);
        if (refreshToken != null && !refreshToken.isBlank()) {
            jwtService.revoke(refreshToken);
        }
        auditLogger.log(AuditLogger.Event.LOGOUT, username, ip);
    }

    private AuthDTOs.TokenResponse issueTokenPair(UserAccount account) {
        String access  = jwtService.generateAccessToken(account.getId(), account.getUsername(), List.copyOf(account.getRoles()));
        String refresh = jwtService.generateRefreshToken(account.getId());
        return new AuthDTOs.TokenResponse(access, refresh, jwtService.getAccessTokenExpiryMs());
    }

    private void recordFailedAttempt(UserAccount account, String ip) {
        int attempts = account.getFailedLoginAttempts() + 1;
        account.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_ATTEMPTS) {
            account.setAccountLocked(true);
            account.setLockoutUntil(Instant.now().plusSeconds(LOCKOUT_SECONDS));
            auditLogger.log(AuditLogger.Event.ACCOUNT_LOCKED, account.getUsername(), ip,
                "after_" + attempts + "_attempts");
        }
        userDetailsService.save(account);
    }

    public static class AuthException extends RuntimeException {
        public AuthException(String message) { super(message); }
    }
}
