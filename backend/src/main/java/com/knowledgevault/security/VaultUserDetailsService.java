package com.knowledgevault.security;

import com.knowledgevault.model.UserAccount;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory UserDetailsService.
 * Replace the maps with JPA/repository calls when adding a real database.
 */
@Service
public class VaultUserDetailsService implements UserDetailsService {

    // username → account  (for login lookups)
    private final Map<String, UserAccount> byUsername = new ConcurrentHashMap<>();
    // userId   → account  (for JWT lookups)
    private final Map<String, UserAccount> byId       = new ConcurrentHashMap<>();

    // ── Registration ──────────────────────────────────────────────────────────

    public void register(UserAccount account) {
        byUsername.put(account.getUsername().toLowerCase(), account);
        byId.put(account.getId(), account);
    }

    public boolean usernameExists(String username) {
        return byUsername.containsKey(username.toLowerCase());
    }

    // ── Spring Security contract ──────────────────────────────────────────────

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = byUsername.get(username.toLowerCase());
        if (account == null) {
            // Use identical timing to prevent username enumeration via timing attacks
            throw new UsernameNotFoundException("Invalid credentials");
        }
        return toUserDetails(account);
    }

    /** Used by JwtAuthFilter to look up by userId from token subject */
    public UserDetails loadUserById(String userId) throws UsernameNotFoundException {
        UserAccount account = byId.get(userId);
        if (account == null) throw new UsernameNotFoundException("User not found");
        return toUserDetails(account);
    }

    /** Returns the raw UserAccount for auth service logic */
    public UserAccount findByUsername(String username) {
        return byUsername.get(username.toLowerCase());
    }

    public UserAccount findById(String id) {
        return byId.get(id);
    }

    public void save(UserAccount account) {
        byUsername.put(account.getUsername().toLowerCase(), account);
        byId.put(account.getId(), account);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private UserDetails toUserDetails(UserAccount account) {
        return User.builder()
            .username(account.getId())        // use ID as principal — not username
            .password(account.getPasswordHash())
            .authorities(account.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()))
            .accountLocked(account.isCurrentlyLocked())
            .disabled(!account.isEnabled())
            .build();
    }
}
