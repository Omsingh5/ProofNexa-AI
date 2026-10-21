# Session handoff

Date: 2026-10-16

## Completed this session

- Audited `C:\Users\Omsin\proofora` and copied it into `C:\Project\ProofNexa AI` (without `.git`).
- Wrote `IMPLEMENTATION_PLAN.md` with an honest gap analysis. Phases 1–5 in the old roadmap are a vertical slice, not the full specification.
- Kept Java package `com.proofora` and MySQL database name `proofora`.
- Renamed user-facing product text to ProofNexa AI.
- Implemented auth hardening:
  - Password strength (8+ chars, upper, lower, digit) on register and reset
  - Hashed single-use password reset tokens (30 minutes)
  - Generic forgot-password response; token returned only when `PROOFORA_EXPOSE_RESET_TOKEN=true`
  - Login rate limit (8 failures / 15 minutes per email) and reset rate limit
  - Account deactivation; disabled users are rejected on login and on JWT requests
  - Logout writes an audit event. The JWT is not revoked and stays valid until expiry unless the account is deactivated
  - Unexpected server errors no longer return `ex.getMessage()`
- Frontend: show/hide password, forgot and reset pages, deactivate control on candidate, recruiter, and admin screens
- `frontend` test script is `vitest run`

## Repository state

Working tree is the copied Proofora app plus the auth milestone. It is not a git repository in this workspace. Original history remains at `C:\Users\Omsin\proofora`.

## Decisions

- Do not rename `com.proofora` until a dedicated change, because it touches every import and does not change behavior.
- `users.enabled` is nullable. Null means enabled so Hibernate `ddl-auto: update` does not lock out existing rows.
- No Flyway yet. New `password_reset_tokens` table is created by Hibernate in dev and tests.
- Email verification and refresh tokens wait until a mail provider exists.

## Pending

- Email delivery for reset tokens
- Candidate profile sections (next milestone)
- Job lifecycle, saved jobs, application state machine
- Flyway and `ddl-auto: validate`
- Organization model
- Async AI jobs
- Package lockfile is present after `npm install` and should be committed with the frontend so GitHub Actions `npm ci` works
- MySQL, Docker, and browser flows were not run
- `mvn` is not on PATH. Use IntelliJ’s bundled Maven or install Maven

## Next task

Expand candidate profiles: education, experience, skills, certifications, projects, phone, LinkedIn, portfolio, and work preferences. Keep ownership checks and add MockMvc tests.

## Validation commands

```powershell
& "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" -B test
```

Run that from `backend`.

```powershell
npm test
```

Run that from `frontend`.

Last results: backend 8 tests passed, frontend 3 tests passed.
