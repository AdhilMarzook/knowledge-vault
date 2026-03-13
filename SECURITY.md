# 🔐 Knowledge Vault — Security Architecture

## Overview

This document describes every security layer in the Knowledge Vault backend.

---

## 1. Authentication — JWT (HS512)

| Property              | Value                                      |
|-----------------------|--------------------------------------------|
| Algorithm             | HMAC-SHA512 (HS512) — strongest symmetric  |
| Access token TTL      | 15 minutes                                 |
| Refresh token TTL     | 7 days                                     |
| Token type claim      | Enforced — refresh tokens rejected as access |
| jti (JWT ID)          | UUID per token — enables revocation        |
| Revocation store      | In-memory ConcurrentHashMap (→ Redis in prod) |
| Refresh rotation      | Old refresh token revoked on every refresh |
| Key minimum length    | 64 chars enforced at startup (HS512 = 512 bits) |

**Token flow:**
```
POST /api/auth/register  →  { accessToken, refreshToken }
POST /api/auth/login     →  { accessToken, refreshToken }
POST /api/auth/refresh   →  { new accessToken, new refreshToken }  (old refresh revoked)
POST /api/auth/logout    →  both tokens revoked
```

---

## 2. Password Security — BCrypt

| Property       | Value                              |
|----------------|------------------------------------|
| Algorithm      | BCrypt                             |
| Cost factor    | 12 (≈300ms per hash — strong)      |
| Min length     | 12 characters                      |
| Max length     | 72 characters (BCrypt safe limit)  |
| Requirements   | Upper + lower + digit + special    |
| Storage        | Hash only — plaintext never stored |

---

## 3. Brute-Force & Lockout Protection

| Property              | Value                              |
|-----------------------|------------------------------------|
| Max failed attempts   | 5 consecutive                      |
| Lockout duration      | 15 minutes (progressive in prod)   |
| Timing-safe compare   | Always runs BCrypt even for unknown users |
| Username enumeration  | Prevented — identical error message for all failures |

---

## 4. Rate Limiting — Bucket4j (Token Bucket Algorithm)

| Endpoint group         | Limit                          |
|------------------------|--------------------------------|
| `/api/auth/**`         | 10 requests / 15 minutes / IP  |
| All other `/api/**`    | 100 requests / 1 minute / IP   |

- Runs before JWT auth filter
- Returns `429 Too Many Requests` with `Retry-After` header
- IP resolved from `X-Forwarded-For` (leftmost IP only)

---

## 5. Input Validation

- `@Valid` + JSR-380 Bean Validation on all request bodies
- `@Pattern` whitelist on skill parameter (only known skill names)
- `selectedIndex` range-checked (0–3) server-side
- Validation errors return field-level messages — never stack traces

---

## 6. Authorization — Spring Security

- All game endpoints require valid JWT (`isAuthenticated()`)
- `@PreAuthorize` on every controller method
- Players can **only** access their own data — `playerId` resolved from JWT, never from request body
- Method-level security enabled (`@EnableMethodSecurity`)
- Leaderboard is the only unauthenticated read

---

## 7. Security Response Headers

| Header                      | Value                                  |
|-----------------------------|----------------------------------------|
| `Strict-Transport-Security` | max-age=31536000; includeSubDomains; preload |
| `X-Frame-Options`           | DENY                                   |
| `X-Content-Type-Options`    | nosniff                                |
| `X-XSS-Protection`          | 1; mode=block                          |
| `Referrer-Policy`           | no-referrer                            |
| `Content-Security-Policy`   | default-src 'none'; frame-ancestors 'none' |
| `Permissions-Policy`        | camera=(), microphone=(), geolocation=(), payment=() |

---

## 8. Error Handling — No Information Leakage

- Custom 401/403 JSON responses (no Spring default error pages)
- `server.error.include-stacktrace=never`
- `server.error.include-message=never`
- `server.error.include-exception=false`
- Global `@RestControllerAdvice` — all exceptions caught
- Full detail logged server-side only

---

## 9. CORS — Strict Configuration

- `allowedOrigins` set from `CORS_ALLOWED_ORIGIN` env var (never `*`)
- Explicit allowed methods: GET, POST, PUT, DELETE, OPTIONS
- Explicit allowed headers only
- `allowCredentials=true` required for cookie-less JWT flow

---

## 10. Docker / Container Hardening

- Multi-stage build — build tools not in runtime image
- Non-root user (`vault:vault`) inside container
- `read_only: true` filesystem + `/tmp` tmpfs
- `no-new-privileges:true` security option
- No root access possible inside container
- JVM flags: `-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage=75`

---

## 11. Secrets Management

- All secrets via environment variables — never hardcoded
- JWT secret enforced minimum 64 chars at startup
- `.env` file excluded from version control via `.gitignore`
- `.env.example` provided with placeholder values only

---

## Production Hardening Checklist

- [ ] Set `JWT_SECRET` to output of `openssl rand -hex 64`
- [ ] Set `CORS_ALLOWED_ORIGIN` to your real frontend domain
- [ ] Replace in-memory stores with PostgreSQL + Redis
- [ ] Move JWT revocation list to Redis (with TTL = token expiry)
- [ ] Enable HTTPS / TLS termination (nginx or load balancer)
- [ ] Set `HSTS` preload in your domain registry
- [ ] Enable structured JSON logging + ship to SIEM
- [ ] Add Dependabot / Renovate for dependency updates
- [ ] Run `mvn dependency-check:check` (OWASP) in CI
- [ ] Consider adding MFA (TOTP) for admin accounts
