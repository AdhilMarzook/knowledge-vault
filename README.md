<div align="center">

```
██╗  ██╗███╗   ██╗ ██████╗ ██╗    ██╗██╗     ███████╗██████╗  ██████╗ ███████╗
██║ ██╔╝████╗  ██║██╔═══██╗██║    ██║██║     ██╔════╝██╔══██╗██╔════╝ ██╔════╝
█████╔╝ ██╔██╗ ██║██║   ██║██║ █╗ ██║██║     █████╗  ██║  ██║██║  ███╗█████╗
██╔═██╗ ██║╚██╗██║██║   ██║██║███╗██║██║     ██╔══╝  ██║  ██║██║   ██║██╔══╝
██║  ██╗██║ ╚████║╚██████╔╝╚███╔███╔╝███████╗███████╗██████╔╝╚██████╔╝███████╗
╚═╝  ╚═╝╚═╝  ╚═══╝ ╚═════╝  ╚══╝╚══╝ ╚══════╝╚══════╝╚═════╝  ╚═════╝ ╚══════╝
                          V A U L T
```

**A Fallout-inspired AI-powered knowledge RPG built on a production-grade,**  
**security-hardened full-stack architecture with multi-provider AI and zero-cost operation.**

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue?style=flat-square&logo=react)](https://react.dev/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)](https://docs.docker.com/compose/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)
[![Security](https://img.shields.io/badge/Security-11_Layers-red?style=flat-square&logo=shield)](SECURITY.md)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
  - [System Diagram](#system-diagram)
  - [Request Lifecycle](#request-lifecycle)
  - [AI Provider Chain](#ai-provider-chain)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Game Design](#game-design)
  - [S.K.I.L.L. System](#skill-system)
  - [Progression Model](#progression-model)
  - [NPC Quest Engine](#npc-quest-engine)
- [Security Architecture](#security-architecture)
- [API Reference](#api-reference)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Environment Configuration](#environment-configuration)
  - [Docker (Recommended)](#docker-recommended)
  - [Local Development](#local-development)
- [AI Provider Setup](#ai-provider-setup)
- [Cost Analysis](#cost-analysis)
- [Production Deployment](#production-deployment)
- [Roadmap](#roadmap)
- [Contributing](#contributing)

---

## Overview

Knowledge Vault is a real-time, multiplayer knowledge RPG where players level up skills by answering AI-generated questions. The game is deliberately designed to have no question bank — every challenge is generated live by large language models, dynamically calibrated to each player's current skill level, ensuring questions grow harder the more you know.

The system is built with a **multi-provider AI fallback chain** that routes requests through free-tier APIs (Groq, Gemini, Mistral, OpenRouter) before falling back to paid providers, enabling **$0/month operation** at personal and small-group scale.

### Key Characteristics

- **Infinite content** — AI generates unique questions on every request, no repeats
- **Adaptive difficulty** — question hardness scales with your skill level (1–100)
- **Zero cost** capable — runs entirely on free-tier AI APIs for personal/small-group use
- **Production-grade security** — JWT authentication, BCrypt hashing, rate limiting, circuit breakers, 11-layer security stack
- **Containerised** — single `docker-compose up --build` deploys the entire stack
- **Stateless backend** — horizontally scalable, JWT-based, no server-side session state

---

## Architecture

### System Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT BROWSER                               │
│                    React 18 SPA (Port 3000)                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│  │  Login/  │ │  Skills  │ │  Quest   │ │  Result  │ │ Leader-  │ │
│  │ Register │ │Dashboard │ │  Screen  │ │  Screen  │ │  board   │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ │
│              Axios + JWT Interceptor + Silent Refresh               │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ HTTPS (Bearer Token)
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│                         NGINX REVERSE PROXY                         │
│               (inside frontend Docker container)                    │
│  /api/** ──► proxy_pass http://backend:8080/api/**                  │
│  /*      ──► serve React SPA (index.html fallback)                  │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ Internal Docker network
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│               SPRING BOOT BACKEND (Java 21, Port 8080)              │
│                                                                     │
│  ┌─────────────────── FILTER CHAIN (ordered) ────────────────────┐  │
│  │  1. RateLimitFilter    (Bucket4j — per-IP token bucket)        │  │
│  │  2. JwtAuthFilter      (HS512 validation + SecurityContext)    │  │
│  │  3. UsernamePasswordAuthenticationFilter (Spring Security)     │  │
│  └────────────────────────────┬──────────────────────────────────┘  │
│                               │                                     │
│  ┌─────────────────── CONTROLLERS ───────────────────────────────┐  │
│  │  AuthController   POST /api/auth/{register,login,refresh,      │  │
│  │                        logout}  GET /api/auth/me              │  │
│  │  GameController   GET  /api/{players/me,leaderboard,quest}    │  │
│  │                   POST /api/answer                            │  │
│  │                   GET  /api/providers/status                  │  │
│  └────────────────────────────┬──────────────────────────────────┘  │
│                               │                                     │
│  ┌─────────────────── SERVICES ──────────────────────────────────┐  │
│  │  AuthService         Registration, login, lockout, JWT mgmt   │  │
│  │  PlayerService       Scoring, skill levelling, leaderboard    │  │
│  │  QuestGenerationService  Prompt building, response parsing    │  │
│  │                                                               │  │
│  │  ┌─────────────── AI PROVIDER ROUTER ──────────────────────┐  │  │
│  │  │  Priority chain with circuit-breaker per provider:       │  │  │
│  │  │  1. GroqProvider       (Llama 3.3-70B, free, ~1k tok/s)  │  │  │
│  │  │  2. GeminiProvider     (Gemini 2.0 Flash, free)          │  │  │
│  │  │  3. MistralProvider    (Mistral Small, free)             │  │  │
│  │  │  4. OpenRouterProvider (Llama 3.3-70B, free)             │  │  │
│  │  │  5. ClaudeProvider     (Haiku 4.5, paid last-resort)     │  │  │
│  │  │  ProviderHealthTracker (3 failures → 5-min cooldown)     │  │  │
│  │  └─────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  ┌─────────────────── SECURITY LAYER ────────────────────────────┐  │
│  │  SecurityConfig  CORS, CSRF, headers, session, auth rules      │  │
│  │  JwtService      HS512 sign/verify, jti revocation             │  │
│  │  AuthService     BCrypt-12, lockout, timing-safe compare       │  │
│  │  GlobalExceptionHandler  Safe errors, no stack trace leakage   │  │
│  └───────────────────────────────────────────────────────────────┘  │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ HTTPS outbound
                               │
         ┌─────────────────────┴──────────────────────┐
         │           EXTERNAL AI PROVIDER APIS         │
         │                                             │
         │  ┌──────────┐ ┌──────────┐ ┌────────────┐  │
         │  │   Groq   │ │ Gemini   │ │  Mistral   │  │
         │  │  (FREE)  │ │  (FREE)  │ │   (FREE)   │  │
         │  └──────────┘ └──────────┘ └────────────┘  │
         │  ┌──────────┐ ┌──────────┐                  │
         │  │OpenRouter│ │  Claude  │                  │
         │  │  (FREE)  │ │  (PAID)  │                  │
         │  └──────────┘ └──────────┘                  │
         └─────────────────────────────────────────────┘
```

### Request Lifecycle

A complete authenticated quest request flows through the following stages:

```
Client                    Nginx               Backend Filters              Services
  │                         │                       │                         │
  │── GET /api/quest ───────►│                       │                         │
  │   Authorization: Bearer  │                       │                         │
  │                         │── proxy_pass ─────────►│                         │
  │                         │                       │                         │
  │                         │              RateLimitFilter                    │
  │                         │              ├─ resolve IP                      │
  │                         │              ├─ check bucket (100/min)          │
  │                         │              └─ consume 1 token                 │
  │                         │                       │                         │
  │                         │              JwtAuthFilter                      │
  │                         │              ├─ extract Bearer token            │
  │                         │              ├─ validate HS512 signature        │
  │                         │              ├─ check jti revocation            │
  │                         │              ├─ verify type = "access"          │
  │                         │              └─ set SecurityContext             │
  │                         │                       │                         │
  │                         │              GameController                     │
  │                         │              ├─ @PreAuthorize check             │
  │                         │              ├─ resolve playerId from JWT       │
  │                         │              └─ whitelist-validate skill        │
  │                         │                                 │               │
  │                         │                         QuestGenerationService  │
  │                         │                         ├─ build prompt         │
  │                         │                         └─ AiProviderRouter ──► Groq API
  │                         │                                 │               │
  │                         │                         parse + validate JSON   │
  │                         │                         store quest in memory   │
  │                         │                                 │               │
  │◄── 200 OK + Quest JSON ─◄│◄─────────────────────────────◄│               │
```

### AI Provider Chain

The `AiProviderRouter` tries providers in priority order. Each provider is independently monitored by `ProviderHealthTracker`, which implements a lightweight circuit-breaker pattern.

```
Quest request arrives
        │
        ▼
┌───────────────────┐
│  Is Groq configured  │──No──►  skip
│  and available?      │
└──────┬────────────┘
       │ Yes
       ▼
┌───────────────────┐
│  Call Groq API   │──error──►  recordFailure() ──► 3rd failure? ──► 5-min cooldown
│  (timeout: 20s)  │
└──────┬────────────┘
       │ Success
       ▼
 recordSuccess()
 return response
       │
       │  (if Groq failed, repeat for Gemini → Mistral → OpenRouter → Claude)
       │
       ▼
  All failed?
       │
       ▼
 Static fallback quest (never fails, no AI needed)
```

**Circuit-breaker states:**

| State | Condition | Behaviour |
|---|---|---|
| CLOSED (normal) | < 3 consecutive failures | Requests flow through |
| OPEN (cooldown) | 3rd failure trips breaker | Skipped for 5 minutes |
| HALF-OPEN (probe) | Cooldown expired | One request allowed through |
| CLOSED (recovered) | Probe succeeds | Fully operational |

---

## Technology Stack

### Backend

| Component | Technology | Version | Purpose |
|---|---|---|---|
| Runtime | Java (OpenJDK) | 21 LTS | Virtual threads (Project Loom), records |
| Framework | Spring Boot | 3.2.0 | Application container, DI, MVC |
| Security | Spring Security | 6.x | Authentication, authorization, filter chain |
| Authentication | JJWT | 0.12.3 | HS512 JWT generation and validation |
| Password Hashing | BCrypt | Cost 12 | Adaptive hashing with salt |
| Rate Limiting | Bucket4j | 8.7.0 | Token-bucket algorithm, per-IP limiting |
| HTTP Client | Spring WebFlux (WebClient) | 6.x | Non-blocking outbound AI API calls |
| Serialisation | Jackson | 2.x | JSON processing, date handling |
| Validation | Hibernate Validator (JSR-380) | 8.x | Bean validation, constraint annotations |
| Build | Maven | 3.9.x | Dependency management, lifecycle |

### Frontend

| Component | Technology | Version | Purpose |
|---|---|---|---|
| Framework | React | 18.2.0 | Component model, hooks, state management |
| HTTP Client | Axios | 1.6.0 | API calls, interceptors, token refresh |
| Styling | CSS Variables + Custom | — | CRT terminal aesthetic, animations |
| Fonts | Share Tech Mono, VT323 | — | Retro terminal typography |
| Build | Create React App | 5.0.1 | Build pipeline, dev server |
| Server | Nginx | Alpine | Static serving, API reverse proxy |

### Infrastructure

| Component | Technology | Purpose |
|---|---|---|
| Containerisation | Docker | Reproducible builds, isolation |
| Orchestration | Docker Compose v3.9 | Multi-service coordination |
| Reverse Proxy | Nginx (Alpine) | API proxying, SPA routing, gzip |
| Networking | Docker bridge network | Internal service-to-service isolation |

### AI Providers

| Provider | Model | Tier | Rate Limit | Use Case |
|---|---|---|---|---|
| Groq | Llama 3.3-70B Versatile | Free | 14,400 req/day | Primary — fastest inference |
| Google Gemini | Gemini 2.0 Flash | Free | ~500 req/day | Fallback 1 |
| Mistral | Mistral Small Latest | Free | 2 req/min | Fallback 2 |
| OpenRouter | Llama 3.3-70B Instruct | Free | Varies | Fallback 3 |
| Anthropic Claude | Haiku 4.5 | Paid | API-key limited | Last resort only |

---

## Project Structure

```
knowledge-vault/
│
├── README.md                          ← This document
├── SECURITY.md                        ← Full security architecture reference
├── .env.example                       ← Environment variable template
├── .gitignore
├── docker-compose.yml                 ← Full-stack orchestration
│
├── backend/                           ← Spring Boot application (Java 21)
│   ├── Dockerfile                     ← Multi-stage build (Maven → JRE Alpine)
│   ├── pom.xml                        ← Maven dependencies + build config
│   └── src/main/
│       ├── resources/
│       │   └── application.properties ← All runtime configuration
│       └── java/com/knowledgevault/
│           │
│           ├── KnowledgeVaultApplication.java   ← Entry point
│           │
│           ├── config/
│           │   └── SecurityConfig.java          ← Spring Security policy (CORS,
│           │                                       headers, filter chain, BCrypt)
│           ├── controller/
│           │   ├── AuthController.java           ← POST /api/auth/{register,login,
│           │   │                                    refresh,logout}  GET /api/auth/me
│           │   └── GameController.java           ← GET /api/{players/me,leaderboard,
│           │                                        quest,providers/status}
│           │                                       POST /api/answer
│           ├── exception/
│           │   └── GlobalExceptionHandler.java  ← Centralised safe error responses
│           │
│           ├── filter/
│           │   ├── JwtAuthFilter.java            ← Bearer token extraction + validation
│           │   ├── RateLimitFilter.java          ← Per-IP token-bucket rate limiting
│           │   └── RequestIdFilter.java          ← Correlation ID injection
│           │
│           ├── model/
│           │   ├── UserAccount.java              ← Auth user (hashed password, roles,
│           │   │                                    lockout state, login history)
│           │   ├── Player.java                   ← Game profile (skills, score, level)
│           │   ├── Quest.java                    ← Generated question + choices
│           │   ├── AnswerRequest.java            ← Client answer submission DTO
│           │   ├── AnswerResult.java             ← Scoring outcome + NPC reaction
│           │   └── auth/
│           │       └── AuthDTOs.java             ← RegisterRequest, LoginRequest,
│           │                                        TokenResponse, UserView
│           ├── security/
│           │   ├── JwtService.java               ← HS512 sign/verify, jti revocation,
│           │   │                                    access/refresh token lifecycle
│           │   ├── AuthService.java              ← Registration, login, lockout,
│           │   │                                    token issuance, logout
│           │   ├── VaultUserDetailsService.java  ← Spring UserDetailsService impl,
│           │   │                                    in-memory user store
│           │   ├── AuditLogger.java              ← Structured security event logging
│           │   └── InputSanitizer.java           ← Input cleaning utilities
│           │
│           └── service/
│               ├── PlayerService.java            ← Score calculation, skill levelling,
│               │                                    quest storage, leaderboard
│               ├── QuestGenerationService.java   ← Prompt construction, AI response
│               │                                    parsing, validation, fallbacks
│               └── provider/
│                   ├── AiProvider.java           ← Provider interface contract
│                   ├── AiProviderRouter.java     ← Fallback chain orchestrator
│                   ├── ProviderHealthTracker.java← Circuit-breaker + success rate tracking
│                   ├── GroqProvider.java         ← Groq API (OpenAI-compatible)
│                   ├── GeminiProvider.java       ← Google Gemini API (OpenAI-compat)
│                   ├── MistralProvider.java      ← Mistral API (OpenAI-compatible)
│                   ├── OpenRouterProvider.java   ← OpenRouter API (OpenAI-compatible)
│                   └── ClaudeProvider.java       ← Anthropic API (native format)
│
└── frontend/                          ← React 18 SPA
    ├── Dockerfile                     ← Node build → Nginx Alpine (multi-stage)
    ├── nginx.conf                     ← API proxy + SPA fallback routing + gzip
    ├── package.json
    └── src/
        ├── index.js                   ← React DOM entry point
        ├── App.jsx                    ← Root component, screen state machine,
        │                                session restoration, logout handler
        ├── styles/
        │   └── global.css            ← CRT terminal design system (CSS variables,
        │                                scanlines, phosphor glow, animations)
        ├── components/
        │   ├── Header.jsx            ← Player stats bar, active AI provider badge,
        │   │                            logout button, leaderboard nav
        │   ├── LoginScreen.jsx       ← Register/login toggle, password strength meter,
        │   │                            real-time policy validation
        │   ├── SkillsScreen.jsx      ← 8-skill dashboard, level bars, difficulty preview
        │   ├── QuestScreen.jsx       ← NPC dialogue, typewriter question effect,
        │   │                            choice selection, answer submission
        │   ├── ResultScreen.jsx      ← Correct/incorrect banner, NPC reaction,
        │   │                            XP/score rewards, level-up celebration
        │   └── LeaderboardScreen.jsx ← Top 10 global rankings, current player highlight
        └── services/
            └── api.js               ← Axios instance, Bearer token interceptor,
                                        silent refresh on 401, token rotation,
                                        all API call functions
```

---

## Game Design

### S.K.I.L.L. System

The **S.K.I.L.L.** (Systematic Knowledge and Intellect Levelling Library) system tracks mastery across 8 disciplines. Each skill operates independently with its own level (1–100) that directly influences question difficulty.

| Skill | Domains Covered | Example Topics |
|---|---|---|
| **Science** | Physics, Chemistry, Biology, Astronomy | Quantum mechanics, periodic table, evolution, cosmology |
| **History** | Ancient, Medieval, Modern, World Events | Empires, wars, treaties, revolutions, cultural movements |
| **Technology** | Computing, Engineering, Innovation | Algorithms, semiconductors, internet protocols, inventions |
| **Philosophy** | Logic, Ethics, Metaphysics, Epistemology | Thought experiments, philosophical schools, paradoxes |
| **Mathematics** | Algebra, Calculus, Statistics, Number Theory | Proofs, theorems, equations, mathematical history |
| **Literature** | Classical, Modern, World Literature, Linguistics | Authors, works, movements, narrative theory |
| **Geography** | Physical, Political, Demographics, Climatology | Nations, capitals, rivers, ecosystems, populations |
| **Politics** | Systems, Theory, International Relations | Governments, treaties, ideologies, global institutions |

### Progression Model

**Skill Levelling**

Each correct answer increases the relevant skill level. Increment magnitude is proportional to question difficulty.

```
newSkillLevel = min(100, currentSkillLevel + difficulty)
```

**Question Difficulty Calculation**

Difficulty adapts automatically as the player advances:

```
difficulty = min(10, max(1, floor(skillLevel / 10) + 1))
```

| Skill Level | Difficulty | Label |
|---|---|---|
| 1–9 | 1 | TRIVIAL |
| 10–19 | 2 | EASY |
| 20–39 | 3–4 | MODERATE |
| 40–59 | 5–6 | AVERAGE / CHALLENGING |
| 60–79 | 7–8 | HARD |
| 80–89 | 9 | EXPERT |
| 90–100 | 10 | MASTER |

**Score and Rewards**

```
xpReward    = difficulty × 50
scoreReward = difficulty × 100
```

**Player Level Formula**

Player level is derived from cumulative score using a square-root curve, ensuring early levels are quick and later levels require sustained effort:

```
playerLevel = 1 + floor(sqrt(totalScore / 500))
```

| Score | Player Level |
|---|---|
| 0 | 1 |
| 500 | 2 |
| 2,000 | 3 |
| 4,500 | 4 |
| 8,000 | 5 |
| 50,000 | 11 |

**Streaks**

Consecutive correct answers increment `currentStreak`. Any wrong answer resets it to 0. Streaks are tracked and displayed in the UI — future versions will apply streak multipliers to score.

### NPC Quest Engine

Every quest is contextualised by an AI-generated NPC character. Eight named NPCs exist, each with unique dialogue matching the in-game Fallout-inspired setting:

| NPC | Role |
|---|---|
| Overseer Vance | Vault administrator |
| Dr. Elara Mote | Scientist |
| Professor Grim | Historian |
| The Archivist | Keeper of old-world records |
| Scout Reyes | Field agent |
| Elder Thornton | Tribal elder |
| Hacker Zero | Technology specialist |
| Sage Miriam | Philosopher |

NPCs deliver contextual opening dialogue before each quest, and a reaction line (praise or reprimand) after the answer is revealed.

---

## Security Architecture

A full treatment is in [SECURITY.md](SECURITY.md). Summary below.

### Authentication Flow

```
Registration:  POST /api/auth/register
               ├─ Validate username format (regex whitelist)
               ├─ Validate password policy (min 12 chars, mixed case, symbols)
               ├─ Check username uniqueness (constant-time to prevent enumeration)
               ├─ BCrypt hash password (cost factor 12)
               ├─ Create UserAccount + linked Player profile
               └─ Return { accessToken (15min), refreshToken (7d) }

Login:         POST /api/auth/login
               ├─ Always run BCrypt.matches() even for unknown users (timing attack prevention)
               ├─ On failure: increment failedAttempts counter
               ├─ 5th failure: lock account for 15 minutes
               └─ On success: reset counter, update lastLoginAt/IP, return token pair

Token Refresh: POST /api/auth/refresh
               ├─ Validate refresh token (type claim, expiry, revocation)
               ├─ Revoke old refresh token (jti added to revocation set)
               └─ Issue new access + refresh token pair (rotation)

Logout:        POST /api/auth/logout
               └─ Revoke both access token and refresh token by jti
```

### Security Layers

| # | Layer | Implementation |
|---|---|---|
| 1 | JWT — HS512 | 64-byte minimum key, 15-min access tokens, per-token jti, refresh rotation |
| 2 | Password security | BCrypt cost 12, 12–72 char policy, complexity enforced |
| 3 | Brute-force protection | 5-attempt lockout, 15-min cooldown, timing-safe comparison |
| 4 | Rate limiting | Bucket4j: 10/15min on auth routes, 100/min general, per-IP |
| 5 | Input validation | JSR-380 Bean Validation, regex whitelist on all parameters |
| 6 | Authorization | @PreAuthorize on every endpoint, playerId from JWT (never request body) |
| 7 | Security headers | HSTS (1yr+preload), CSP, X-Frame DENY, Referrer no-referrer |
| 8 | Error handling | No stack traces, no class names, no exceptions in responses |
| 9 | CORS | Strict origin allowlist from env var, no wildcard |
| 10 | Container hardening | Non-root user, read-only filesystem, no-new-privileges |
| 11 | Secrets | All via env vars, 64-char JWT minimum enforced at startup |

---

## API Reference

### Authentication Endpoints

All auth endpoints are rate-limited to **10 requests per 15 minutes per IP**.

#### `POST /api/auth/register`

Register a new vault dweller account.

**Request body:**
```json
{
  "username": "vault_dweller_01",
  "password": "Str0ng!PassWord99"
}
```

**Constraints:**
- `username`: 3–20 chars, pattern `^[a-zA-Z0-9_-]+$`
- `password`: 12–72 chars, must contain uppercase, lowercase, digit, special character

**Response `201 Created`:**
```json
{
  "accessToken":  "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "accessExpiresIn": 900000,
  "tokenType": "Bearer"
}
```

---

#### `POST /api/auth/login`

```json
{ "username": "vault_dweller_01", "password": "Str0ng!PassWord99" }
```

**Response `200 OK`:** Same as register.  
**Error `401`:** Invalid credentials (identical message for wrong password and unknown user).  
**Error `401`:** Account locked — after 5 failed attempts.

---

#### `POST /api/auth/refresh`

Exchange a valid refresh token for a new token pair. The old refresh token is revoked.

```json
{ "refreshToken": "eyJhbGci..." }
```

**Response `200 OK`:** New `{ accessToken, refreshToken, ... }`.

---

#### `POST /api/auth/logout`

Revoke both tokens. Requires Bearer token in Authorization header.

```json
{ "refreshToken": "eyJhbGci..." }
```

**Response `200 OK`:** `{ "message": "Logged out successfully" }`

---

#### `GET /api/auth/me`

Returns the authenticated user's profile. Requires valid Bearer token.

**Response `200 OK`:**
```json
{ "id": "uuid", "username": "vault_dweller_01", "playerId": "uuid" }
```

---

### Game Endpoints

All game endpoints require `Authorization: Bearer <accessToken>`.

#### `GET /api/players/me`

Returns the authenticated player's full game profile.

**Response `200 OK`:**
```json
{
  "id": "uuid",
  "username": "vault_dweller_01",
  "level": 4,
  "totalScore": 4200,
  "questsCompleted": 42,
  "currentStreak": 7,
  "skills": {
    "Science": 35,
    "History": 12,
    "Technology": 61,
    "Philosophy": 5,
    "Mathematics": 22,
    "Literature": 8,
    "Geography": 14,
    "Politics": 3
  },
  "createdAt": "2077-10-23T08:00:00Z",
  "lastActive": "2077-10-23T12:34:56Z"
}
```

---

#### `GET /api/leaderboard`

Returns top 10 players sorted by total score. Public — no authentication required.

**Response `200 OK`:** Array of player objects (subset of fields).

---

#### `GET /api/quest?skill={skill}`

Generates a new AI quest for the authenticated player in the specified skill.

**Parameters:**
- `skill` (required): One of `Science`, `History`, `Technology`, `Philosophy`, `Mathematics`, `Literature`, `Geography`, `Politics`

**Response `200 OK`:**
```json
{
  "id": "uuid",
  "skill": "Technology",
  "difficulty": 7,
  "question": "Which data structure guarantees O(1) average-case lookup?",
  "choices": ["Binary Search Tree", "Hash Table", "Linked List", "Stack"],
  "correctIndex": 1,
  "explanation": "Hash tables use hash functions to achieve O(1) average lookup.",
  "xpReward": 350,
  "scoreReward": 700,
  "npcName": "Hacker Zero",
  "npcDialogue": "The wasteland will test your Technology. Are you ready?"
}
```

---

#### `POST /api/answer`

Submit an answer to an active quest.

**Request body:**
```json
{ "questId": "uuid", "selectedIndex": 1 }
```

**Constraints:** `selectedIndex` must be 0–3. `questId` must belong to the authenticated player.

**Response `200 OK`:**
```json
{
  "correct": true,
  "explanation": "Hash tables use hash functions to achieve O(1) average lookup.",
  "xpEarned": 350,
  "scoreEarned": 700,
  "newSkillLevel": 68,
  "leveledUp": false,
  "newPlayerLevel": 4,
  "npcReaction": "Impressive, Vault Dweller. Your mind is a weapon."
}
```

---

#### `GET /api/providers/status`

Returns the current health of all configured AI providers. Requires authentication.

**Response `200 OK`:**
```json
{
  "activeProvider": "Groq (Llama-3.3-70B) [FREE]",
  "providers": {
    "Groq (Llama-3.3-70B) [FREE]": {
      "configured": true,
      "health": {
        "available": true,
        "totalCalls": 142,
        "successRate": "99%",
        "cooldownUntil": "none"
      }
    },
    "Google Gemini 2.0 Flash [FREE]": { "configured": true, "health": { ... } },
    "Mistral Small [FREE - rate limited]": { "configured": false, "health": { ... } }
  }
}
```

---

#### `GET /api/health`

Public health check. Returns active AI provider name.

**Response `200 OK`:**
```json
{ "status": "ONLINE", "aiProvider": "Groq (Llama-3.3-70B) [FREE]" }
```

---

### Error Response Format

All errors follow a consistent JSON schema — no stack traces, no internal class names:

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid credentials",
  "timestamp": "2077-10-23T12:34:56.789Z"
}
```

Validation errors include a `details` field:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "timestamp": "...",
  "details": {
    "password": "Password must contain uppercase, lowercase, digit and special character",
    "username": "Username must be 3–20 characters"
  }
}
```

---

## Getting Started

### Prerequisites

| Tool | Minimum Version | Notes |
|---|---|---|
| Docker | 24.x | Required for Docker deployment |
| Docker Compose | 2.x (v3.9 syntax) | Included with Docker Desktop |
| Java JDK | 21 | For local development only |
| Maven | 3.9.x | For local development only |
| Node.js | 18.x | For local frontend development only |

### Environment Configuration

Copy the example file and populate your values:

```bash
cp .env.example .env
```

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | **Yes** | Minimum 64 characters. Generate: `openssl rand -hex 64` |
| `CORS_ALLOWED_ORIGIN` | No | Default: `http://localhost:3000` |
| `AI_GROQ_API_KEY` | At least one AI key | [console.groq.com](https://console.groq.com) — free |
| `AI_GEMINI_API_KEY` | At least one AI key | [aistudio.google.com/apikey](https://aistudio.google.com/apikey) — free |
| `AI_MISTRAL_API_KEY` | At least one AI key | [console.mistral.ai](https://console.mistral.ai) — free |
| `AI_OPENROUTER_API_KEY` | At least one AI key | [openrouter.ai](https://openrouter.ai) — free |
| `AI_CLAUDE_API_KEY` | No | [console.anthropic.com](https://console.anthropic.com) — paid fallback |

> **Security:** Never commit `.env` to version control. It is listed in `.gitignore`.

### Docker (Recommended)

```bash
# 1. Clone the repository
git clone https://github.com/your-org/knowledge-vault.git
cd knowledge-vault

# 2. Configure environment
cp .env.example .env
# Edit .env — set JWT_SECRET and at least one AI_*_API_KEY

# 3. Build and launch
docker-compose up --build

# 4. Access the application
# Frontend: http://localhost:3000
# Backend:  http://localhost:8080/api/health
```

**Useful commands:**

```bash
# Run in background
docker-compose up -d --build

# View logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Restart backend only (after code changes)
docker-compose up -d --build backend

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Local Development

#### Backend

```bash
cd backend

# Set required environment variables
export JWT_SECRET=$(openssl rand -hex 64)
export AI_GROQ_API_KEY=your-groq-key
export CORS_ALLOWED_ORIGIN=http://localhost:3000

# Run with Maven
mvn spring-boot:run

# Or build and run the JAR
mvn clean package -DskipTests
java -jar target/knowledge-vault-backend.jar
```

The backend starts on `http://localhost:8080`.

#### Frontend

```bash
cd frontend

# Install dependencies
npm install

# Start development server (proxies /api to localhost:8080)
npm start
```

The frontend starts on `http://localhost:3000` with hot-reload.

---

## AI Provider Setup

You need **at least one** free API key to run the game. Recommended setup for zero-cost operation:

### Step 1 — Groq (Primary, Fastest)

1. Sign up at [console.groq.com](https://console.groq.com) — no credit card required
2. Create an API key in the console
3. Add to `.env`: `AI_GROQ_API_KEY=gsk_...`

### Step 2 — Google Gemini (Fallback)

1. Sign up at [aistudio.google.com](https://aistudio.google.com/apikey) — free Google account
2. Click "Get API Key"
3. Add to `.env`: `AI_GEMINI_API_KEY=AIza...`

### Step 3 — Optional Additional Providers

```bash
# Mistral — free, unlimited but rate-limited (2 req/min)
# Sign up: https://console.mistral.ai
AI_MISTRAL_API_KEY=...

# OpenRouter — free models via single key
# Sign up: https://openrouter.ai
AI_OPENROUTER_API_KEY=sk-or-...

# Claude (paid) — last-resort fallback only
# Sign up: https://console.anthropic.com
AI_CLAUDE_API_KEY=sk-ant-...
```

### Provider Verification

After starting the application, verify your providers are configured and healthy:

```bash
# After logging in and getting a token:
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/providers/status
```

---

## Cost Analysis

### Per-Request Cost

Each quest generation makes one API call with approximately:
- **Input:** ~200 tokens (system prompt + skill/difficulty context)
- **Output:** ~150 tokens (question, 4 choices, explanation as JSON)

| Provider | Input $/M tokens | Output $/M tokens | Cost per quest |
|---|---|---|---|
| Groq (Llama 3.3-70B) | Free | Free | **$0.000** |
| Google Gemini 2.0 Flash | Free | Free | **$0.000** |
| Mistral Small | Free | Free | **$0.000** |
| OpenRouter (Llama free) | Free | Free | **$0.000** |
| Claude Haiku 4.5 | $1.00/M | $5.00/M | **~$0.001** |

### Monthly Cost Projections

| Scale | Players | Quests/Day | Free Provider | Paid Fallback Only |
|---|---|---|---|---|
| Solo | 1 | 20 | **$0** | ~$0.60 |
| Friend group | 10 | 100 | **$0** | ~$3.00 |
| Community | 100 | 1,000 | **$0** | ~$30.00 |
| Small platform | 1,000 | 10,000 | **$0** | ~$300.00 |

> With Groq + Gemini configured, the game runs at **$0/month** until you exceed their combined free-tier limits (~15,000 requests/day total).

---

## Production Deployment

The following changes are recommended before deploying to a production environment.

### 1. Database Persistence

The current implementation uses in-memory `ConcurrentHashMap` stores. Data is lost on restart. Replace with:

- **PostgreSQL** via Spring Data JPA for `UserAccount` and `Player`
- **Redis** for JWT revocation list (with TTL = token expiry) and rate-limit buckets

### 2. TLS / HTTPS

Terminate TLS at a load balancer or with Nginx:

```nginx
server {
    listen 443 ssl;
    ssl_certificate     /etc/ssl/certs/cert.pem;
    ssl_certificate_key /etc/ssl/private/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
}
```

### 3. Environment

```bash
# Use a secrets manager in production (AWS Secrets Manager, HashiCorp Vault, etc.)
# Never pass secrets as plain environment variables in production container platforms

# Regenerate JWT secret for production
JWT_SECRET=$(openssl rand -hex 64)

# Set your real frontend domain
CORS_ALLOWED_ORIGIN=https://yourdomain.com
```

### 4. Production Checklist

- [ ] Generate fresh `JWT_SECRET` with `openssl rand -hex 64`
- [ ] Set `CORS_ALLOWED_ORIGIN` to production domain
- [ ] Replace in-memory stores with PostgreSQL + Redis
- [ ] Configure TLS termination
- [ ] Enable structured JSON logging and ship to a SIEM
- [ ] Run `mvn dependency-check:check` (OWASP CVE scan) in CI
- [ ] Set up Dependabot or Renovate for automated dependency updates
- [ ] Register HSTS preload at [hstspreload.org](https://hstspreload.org)
- [ ] Configure container resource limits (`mem_limit`, `cpus`) in docker-compose

---

## Roadmap

### v1.1 — Persistence
- [ ] PostgreSQL integration via Spring Data JPA
- [ ] Redis JWT revocation + distributed rate limiting
- [ ] User data survives restarts

### v1.2 — Multiplayer Features
- [ ] WebSocket real-time leaderboard updates
- [ ] Head-to-head challenge mode (same question, race to answer)
- [ ] Guilds / teams with shared leaderboards

### v1.3 — Progression Depth
- [ ] Streak multipliers on score rewards
- [ ] Skill trees with specialisation paths
- [ ] Achievement system and badges
- [ ] Daily / weekly challenge quests

### v1.4 — Platform
- [ ] Admin dashboard (user management, provider health monitoring)
- [ ] OAuth2 social login (Google, GitHub)
- [ ] Mobile-responsive layout improvements
- [ ] Dark/light mode toggle

### v2.0 — Scale
- [ ] Kubernetes deployment manifests
- [ ] Horizontal pod autoscaling
- [ ] CDN integration for frontend assets
- [ ] Multi-region support

---

## Contributing

Contributions are welcome. Please follow these guidelines:

1. **Fork** the repository and create a feature branch: `git checkout -b feature/your-feature`
2. **Follow** the existing code style — Spring conventions for backend, functional React for frontend
3. **Write tests** for new service-layer logic
4. **Run security checks** — `mvn dependency-check:check` before submitting
5. **Open a pull request** with a clear description of the change and its purpose

### Development Standards

- Java: Google Java Style Guide
- React: functional components + hooks only, no class components
- Commits: Conventional Commits format (`feat:`, `fix:`, `docs:`, `chore:`)
- All secrets via environment variables — no hardcoded credentials ever

---

<div align="center">

**Built with a Vault-Tec terminal aesthetic and a genuine obsession with clean architecture.**

*"War never changes. But knowledge does."*

**— VAULT-TEC KNOWLEDGE INITIATIVE, 2077**

</div>
