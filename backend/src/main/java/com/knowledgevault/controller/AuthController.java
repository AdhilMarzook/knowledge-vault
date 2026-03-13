package com.knowledgevault.controller;

import com.knowledgevault.model.UserAccount;
import com.knowledgevault.model.auth.AuthDTOs;
import com.knowledgevault.security.AuthService;
import com.knowledgevault.security.AuditLogger;
import com.knowledgevault.security.JwtService;
import com.knowledgevault.security.VaultUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService             authService;
    private final VaultUserDetailsService userDetailsService;
    private final JwtService              jwtService;
    private final AuditLogger             auditLogger;

    public AuthController(
        AuthService authService,
        VaultUserDetailsService userDetailsService,
        JwtService jwtService,
        AuditLogger auditLogger
    ) {
        this.authService        = authService;
        this.userDetailsService = userDetailsService;
        this.jwtService         = jwtService;
        this.auditLogger        = auditLogger;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDTOs.TokenResponse> register(
        @Valid @RequestBody AuthDTOs.RegisterRequest request,
        HttpServletRequest httpRequest
    ) {
        AuthDTOs.TokenResponse tokens = authService.register(request, resolveIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(tokens);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTOs.TokenResponse> login(
        @Valid @RequestBody AuthDTOs.LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(authService.login(request, resolveIp(httpRequest)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDTOs.TokenResponse> refresh(
        @Valid @RequestBody AuthDTOs.RefreshRequest request
    ) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
        @RequestHeader("Authorization") String authHeader,
        @RequestBody(required = false) AuthDTOs.RefreshRequest body,
        @AuthenticationPrincipal UserDetails userDetails,
        HttpServletRequest httpRequest
    ) {
        String accessToken  = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : "";
        String refreshToken = (body != null) ? body.getRefreshToken() : null;
        String username     = userDetails != null ? userDetails.getUsername() : "unknown";
        authService.logout(accessToken, refreshToken, username, resolveIp(httpRequest));
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    /** Safe user view — never exposes passwordHash */
    @GetMapping("/me")
    public ResponseEntity<AuthDTOs.UserView> me(@AuthenticationPrincipal UserDetails userDetails) {
        UserAccount account = userDetailsService.findById(userDetails.getUsername());
        if (account == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(new AuthDTOs.UserView(
            account.getId(), account.getUsername(), account.getPlayerId()
        ));
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
