# Rate Limiting (API Throttling)

Production-visible rate limiting for the TeasJul API, implemented as a plain
servlet filter that runs **after** the Spring Security chain, so it only ever
sees requests that security already approved (401/403 responses are never
throttled, and the authenticated principal is still available for per-user
keys without a DB lookup).

## Architecture

```
Request ─► Spring Security chain (order -100) ─► RateLimitingFilter (order 0) ─► Controller
                                                       │
                    resolve(method+path) → list of checks (RateLimitRuleResolver)
                    for each check: effective policy ─► BucketStore.tryConsume(key, capacity, refill)
                                                       │  └─ CaffeineBucketStore (bucket4j guaranteed-rate)
                    429 / 503 → JSON ErrorResponse + Retry-After
```

All runtime beans are declared as `@Bean` methods in
`ratelimit.RateLimitFilterRegistration` (`@Configuration` +
`@EnableConfigurationProperties(RateLimitProperties.class)`). They are
deliberately **not** `@Component` classes so `@WebMvcTest` slice tests (which
never scan this configuration class) stay untouched.

### Key concept

- **Scope** decides what a bucket is keyed on:
  - `IP` — resolved client IP (see proxy note).
  - `ACCOUNT` — SHA-256 hash of an account identifier read from the JSON body
    (never used raw, never logged).
  - `USER` — SHA-256 hash of the authenticated username from the security context.
- A request can match several checks (e.g. login hits both `AUTH_LOGIN_IP` and
  `AUTH_LOGIN_ACCOUNT`); **all** must pass or the request is rejected.
- Buckets are **token buckets refilled greedily** (bucket4j
  `Bandwidth.classic`), so bursts up to `capacity` are allowed and the limit
  recovers smoothly over the refill period.
- The store is backed by **Caffeine** (expire-after-access + max-life capped),
  keyed by namespaces `{policy}:ip:…`, `{policy}:acct:…`, `{policy}:user:…`.
- When an ACCOUNT-scope check applies, the body is buffered once into a
  `CachedBodyHttpServletRequest` so controllers can still read it afterwards.

### Important: there is no managed `com.fasterxml` ObjectMapper in this app

Spring Boot 4 registers Jackson as `tools.jackson` (its mapper bean is not a
`com.fasterxml.jackson.databind.ObjectMapper`). The rate-limit classes
therefore self-manage a `JsonMapper` exactly like the rest of the codebase
(`OwnerSettingServiceImpl`, `NotificationServiceImpl`), with `JavaTimeModule`
enabled for `ErrorResponse.timestamp`. Do **not** reintroduce constructor
injection of `ObjectMapper` into these classes.

## Default configuration

All defaults live in `src/main/resources/application.properties` and are
overridable via environment variables when the app is launched with the `Dotenv`
loader.

| Property | Env var | Default | Meaning |
|---|---|---|---|
| `app.ratelimit.enabled` | `RATE_LIMIT_ENABLED` | `true` | Master switch (before any check). |
| `app.ratelimit.store.max-buckets` | `RATE_LIMIT_MAX_BUCKETS` | `100000` | Upper cache size for buckets. |
| `app.ratelimit.store.bucket-idle` | `RATE_LIMIT_BUCKET_IDLE` | `30m` | Expire a bucket after this idle time. |
| `app.ratelimit.store.bucket-max-life` | `RATE_LIMIT_BUCKET_MAX_LIFE` | `1h` | Hard max lifetime of a bucket. |
| `app.ratelimit.proxy.trust-forwarded-headers` | `RATE_LIMIT_TRUST_FORWARDED_HEADERS` | `false` | Trust `X-Forwarded-For` for the client IP. **Keep false unless behind a trusted reverse proxy you control** (prevents IP spoofing). |

Per-policy overrides use `app.ratelimit.policies.<NAME>.{capacity,refill-period,fail-closed}`;
in production you can set e.g. `RATE_LIMIT_AUTH_LOGIN_ACCOUNT_CAPACITY=10`.

### Login rate limiting managed from the SUPER_ADMIN settings screen

The two login policies are additionally driven at runtime by the existing
`system_settings` rows, editable from **Admin → Settings → Login Rate
Limiting** (`GET`/`PUT /api/admin/settings/security/login-rate-limit`,
SUPER_ADMIN only):

| Setting key | Default | Effect on the limiter |
|---|---|---|
| `LOGIN_RATE_LIMIT_ENABLED` | `true` | `false` skips **both** login checks entirely; every other policy is untouched. |
| `LOGIN_MAX_FAILED_ATTEMPTS` | `5` | Capacity of the per-account `AUTH_LOGIN_ACCOUNT` bucket (1–100). |
| `LOGIN_LOCKOUT_DURATION_MINUTES` | `15` | Refill window of that bucket, i.e. how long the account stays locked out (1–1440). |

These rows win over `app.ratelimit.policies.AUTH_LOGIN_ACCOUNT.*` (which remains
the fallback default), are read fresh on each login request, and survive restarts.
`AUTH_LOGIN_IP` keeps its configured capacity because the screen edits the
per-account lockout, not the per-IP scraping guard. Missing or unparseable rows
fall back to the defaults above, so protection is never silently disabled.

## Policies (defaults)

| Policy | Capacity / refill | Fail-safe | Target |
|---|---|---|---|
| `AUTH_LOGIN_IP` | 20 / 15m | fail-closed | Failed-login scraping per IP |
| `AUTH_LOGIN_ACCOUNT` | 5 / 15m | fail-closed | Credential stuffing per account |
| `AUTH_REGISTER_IP` | 10 / 1h | fail-closed | Signup flooding |
| `AUTH_OTP_IP` | 30 / 15m | fail-closed | OTP endpoint abuse |
| `AUTH_OTP_ACCOUNT` | 8 / 15m | fail-closed | OTP guessing per account |
| `AUTH_PASSWORD_RESET_IP` | 10 / 15m | fail-closed | Reset smuggling |
| `AUTH_PASSWORD_RESET_ACCOUNT` | 5 / 15m | fail-closed | Reset-per-account |
| `AUTH_2FA_IP` | 20 / 15m | fail-closed | 2FA brute force |
| `AUTH_2FA_ACCOUNT` | 8 / 15m | fail-closed | 2FA brute force per account |
| `PUBLIC_READ` | 120 / 1m | fail-open | Generic public catalogue reads |
| `PUBLIC_DETAIL` | 60 / 1m | fail-open | Public listing/room detail pages |
| `PUBLIC_CONTACT` | 10 / 1h | fail-open | Tenant invitation consumption (public one-shot) |
| `AUTHENTICATED_API_IP` | 1200 / 1m | fail-open | General authenticated calls per IP |
| `AUTHENTICATED_API_USER` | 600 / 1m | fail-open | General authenticated calls per user |
| `SENSITIVE_WRITE_USER` | 30 / 1m | fail-open | Payments, manual payments, feedback, tenant invites |
| `PAYMENT_CALLBACK` | 120 / 1m | fail-open | PayWay HMAC-signed callback (generous, IP-anchored) |

## Endpoint → policies

| Endpoint | Policies applied |
|---|---|
| `OPTIONS *` / non-`/api` paths | none (exempt) |
| `GET /api/public/*` | `PUBLIC_READ` (or `PUBLIC_DETAIL` for `/api/public/rooms/{id}`, `/api/public/listings/*`) |
| `GET /api/v1/tenant-invitations/validate` | `PUBLIC_READ` |
| `POST /api/v1/auth/login` | `AUTH_LOGIN_IP`, `AUTH_LOGIN_ACCOUNT` |
| `POST /api/v1/auth/register` | `AUTH_REGISTER_IP` |
| `POST /api/v1/auth/forgot-password` | `AUTH_PASSWORD_RESET_IP`, `AUTH_PASSWORD_RESET_ACCOUNT` |
| `POST /api/v1/auth/forgot-password/verify-otp` | `AUTH_OTP_IP`, `AUTH_OTP_ACCOUNT` |
| `POST /api/v1/auth/reset-password` | `AUTH_PASSWORD_RESET_IP` (IP-only; token is per-request) |
| `POST /api/v1/auth/2fa/verify` | `AUTH_2FA_IP`, `AUTH_2FA_ACCOUNT` |
| `POST /api/v1/security/recovery-email`, `/verify` | `AUTH_OTP_IP`, `AUTH_OTP_ACCOUNT` |
| `POST /api/payments`, `/api/payments/manual`, `/api/owner/feedback`, `/api/v1/tenant-invitations` | `AUTHENTICATED_API_IP`, `AUTHENTICATED_API_USER`, `SENSITIVE_WRITE_USER` |
| `POST /api/v1/tenant-invitations/consume` | `PUBLIC_CONTACT` |
| `POST /api/payments/callback` | `PAYMENT_CALLBACK` |
| every other `/api` request | `AUTHENTICATED_API_IP`, `AUTHENTICATED_API_USER` |

Account identifiers for ACCOUNT checks come from the body field per endpoint:
`usernameOrEmail` (login), `email` (forgot-password + verify-otp),
`challengeToken` (2fa verify), `recoveryEmail` (recovery-email + verify).
Identifiers are trimmed, lowercased, then SHA-256-hashed (`SecurityCodeUtil.hashSecret`).

## Failure and error responses

- **429 Too Many Requests** — `{"timestamp":ISO8601,"status":429,"error":"Too Many Requests","message":"Too many requests. Please try again later.","path":…}` plus a `Retry-After` header (`>= 1s`). The request never reaches the controller.
- **503 Service Unavailable** — only when the store throws and the offending policy is **fail-closed** (auth surface protection at any cost).
- **Fail-open** policies: store exception ⇒ request proceeds and the error is logged — a limiter hiccup can never take the product down.
- The filter skips `ERROR`/`ASYNC`/`FORWARD` dispatches and is disabled entirely when `app.ratelimit.enabled=false`.
- Logs never contain raw credentials, OTPs, tokens, identifiers or secrets.

## Tests

- `src/test/java/.../ratelimit/` — 50 unit tests covering policy defaults and
  overrides, resolver mappings, client-IP resolution (incl. proxy trust),
  identity extraction, the bucket store (including a 100-worker/50-capacity
  concurrency guarantee), and filter behavior (429 + Retry-After, per-IP and
  per-account independence, callback bypass, body re-read, per-user limits,
  fail-open/fail-closed, master switch, no secret leakage).
- `src/test/java/.../service/RateLimiterServiceTest.java` — legacy
  `RateLimiterService.tryAcquire` still delegating to the same store.
- Full suite: `./mvnw test` → **843 tests, 0 failures** (includes the three
  full-context integration tests).

## Operational notes / multi-instance

- The in-memory Caffeine store is **per-instance** — accurate only for a single
  JVM. For multiple instances behind a load balancer, implement `BucketStore`
  with a shared provider (e.g. Redis) — nothing else in the filter wiring
  changes, since all consumers depend on the `BucketStore` interface only.
- Keep `trust-forwarded-headers=false` unless requests come through a trusted
  reverse proxy; otherwise clients can spoof `X-Forwarded-For`.

---

## 12-point capability report

1. **16 named policies** with sensible defaults for capacity, refill period, and fail-safe mode, covering auth, public, authenticated, sensitive-write and payment-callback traffic.
2. **Every auth surface throttled**: login, register, OTP verify, password reset, 2FA verify, and recovery-email changes — all with per-IP + (where sensible) per-account buckets, evaluated **before** BCrypt/OTP work.
3. **Per-account limiting** keyed on hashed body identifiers (`usernameOrEmail`, `email`, `challengeToken`, `recoveryEmail`) — no plaintext identifiers stored or logged, reset-password deliberately IP-only.
4. **Per-user authenticated limits** keyed on the hashed principal username from the security context, plus a general per-IP ceiling for all `/api` traffic.
5. **Public API throttling** with separate read/detail/contact tiers so scraping can’t take down the catalogue.
6. **Sensitive write operations** (payments, manual payments, feedback, tenant invitations) get an extra per-user throttle.
7. **Payment callback exemption**: the HMAC-signed PayWay callback has its own generous, IP-anchored policy and is never dropped by other checks.
8. **Guaranteed-rate token buckets** (bucket4j + Caffeine) with real `Retry-After`; all knobs configurable via `app.ratelimit.*`/`RATE_LIMIT_*` env vars, master kill switch included.
9. **Fail-safe semantics per policy**: fail-closed auth surface (503 on store failure) vs fail-open business APIs (allow + log) so outages are never amplified.
10. **Proxy-spoofing protection**: forwarded headers trusted only when explicitly enabled.
11. **Safe pipeline placement**: runs after Spring Security (rejected requests never throttled), skips OPTIONS/internal dispatches, and preserves the request body for downstream controllers.
12. **51 new green tests** plus full-suite validation (843 tests, 0 failures); minimal blast radius — `SecurityConfig` and business logic untouched, legacy `RateLimiterService` API preserved.