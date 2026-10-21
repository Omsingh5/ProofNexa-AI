# ProofNexa AI — Implementation Plan

Audit date: 2026-10-16. Source of truth: the existing Proofora codebase copied into this workspace from `C:\Users\Omsin\proofora`. The workspace itself was empty. Working code was preserved. Java packages remain `com.proofora` so imports, tests, and any existing MySQL schema stay compatible. User-facing branding is **ProofNexa AI**.

## Existing architecture

Modular monolith, not a set of empty module folders:

| Layer | What exists |
| --- | --- |
| API | Spring Boot 3.3.5, Java language level 17, Maven. Runtime used for the baseline test was Java 22.0.1. |
| Persistence | Spring Data JPA / Hibernate. `spring.jpa.hibernate.ddl-auto=update`. No Flyway migrations. MySQL 8 in Docker Compose. Tests use H2. |
| Security | Stateless JWT (jjwt 0.12.6), BCrypt, role checks in services, method security on AI endpoints. CSRF disabled because the API is bearer-token based. |
| AI | `AiGateway` calls an OpenAI-compatible HTTP API when `OPENAI_API_KEY` is set. Otherwise deterministic heuristics. No Spring AI dependency. |
| GitHub | Public repository metadata and a bounded file sample via the GitHub REST API. Code is read as text and never executed. |
| UI | React 18, Vite 5, React Router 6, one CSS file. No Tailwind, TanStack Query, React Hook Form, Zod, or Recharts. |
| Tests | `AuthAndJobsTest`, `SkillTaxonomyTest`, one Vitest session helper. No Testcontainers, Playwright, or React Testing Library. |
| Delivery | Dockerfiles, Compose, GitHub Actions CI. CI frontend job runs `npm ci`, but the repo had no `package-lock.json` before this session. |

Controllers live under `web`, entities under `domain`, and most business rules under `service`. DTOs are records in `Dtos.java`. This is a working vertical slice, not the full module tree in the master specification.

## Baseline validation (before this session’s feature work)

| Check | Result |
| --- | --- |
| `mvn -B -DskipITs test` in `backend` | BUILD SUCCESS. Tests run: 3, failures: 0. Maven was the IntelliJ bundled Maven 3 binary; `mvn` is not on PATH. |
| `npm test` in `frontend` | 1 test passed. The script was `vitest` in watch mode, so it did not exit. |
| Live MySQL, OpenAI, GitHub OAuth, email | Not exercised. |

## Feature status against the seven phases

Legend: **slice** = a real path exists and was read in code. **unverified** = implemented, not covered by an automated test and not run against MySQL or a live provider in this session. **partial** = missing spec behavior. **missing** = not in the repo.

### Phase 1 — Core application

| Area | Status |
| --- | --- |
| Register, login, JWT, role guard, BCrypt | Slice. Login/register covered by `AuthAndJobsTest`. |
| Logout | Client-only before this milestone. JWT stayed valid until expiry. |
| Password reset, strength rules, visibility toggle, login rate limit, deactivation | Missing before this milestone. Implemented in the auth milestone below. |
| Email verification, refresh-token rotation | Missing. Blocked on a mail provider for real delivery. |
| Candidate profile | Partial. Headline, summary, location, years, GitHub username, one resume file. No education, experience rows, certifications, portfolio projects, phone, LinkedIn, or work preferences. |
| Recruiter / organization | Partial. Company name and website on `RecruiterProfile`. No organization entity, membership, or verification workflow. |
| Jobs | Partial. Create, edit, search, filter, paginate open jobs. Skills stored as a JSON list. No drafts workflow beyond a status field, salary, openings, deadline, preferred skills, or saved jobs. |
| Applications | Partial. Apply, list, withdraw, status update, timeline. Statuses are `APPLIED, REVIEWING, INTERVIEW, OFFER, REJECTED, WITHDRAWN`. Any status can be written. Events do not store an actor. One resume per profile, not chosen per application. |
| Dashboards | Partial. Candidate home is the profile panel plus recommendation cards. Recruiter and admin screens call live endpoints. They are not the metric dashboards in the spec. |
| MySQL | Unverified in this session. Compose file exists. Schema is Hibernate `ddl-auto: update`, which is unsafe for production. |

### Phase 2 — Essential AI

| Area | Status |
| --- | --- |
| Resume text extraction (PDFBox) and skill lists | Slice, unverified against sample PDFs. |
| Candidate skill correction | Slice, unverified. |
| Skill-gap vs one job | Slice. Wording can still look like a numeric match score (`matchScore` percent on recommendations). |
| Recommendations | Slice, heuristic or model. Formula is not documented in the API payload. |
| Analysis history with model metadata, duration, and job state | Partial. Extractions and gap reports are stored. No `PENDING/RUNNING/FAILED` job table, no async processing. |

### Phase 3 — Skill proof and career growth

| Area | Status |
| --- | --- |
| Public GitHub URL import and reanalyze | Slice, unverified live. No OAuth connection. Token is optional and server-side only. |
| Technology detection | Partial. Regex and manifest heuristics for a fixed taxonomy, not JavaParser. |
| Evidence rows with file references | Slice. |
| Roadmaps and challenges | Slice, largely templated. Completion is a status flag, not evidence of completion. |
| Growth charts from stored records | Missing. |

### Phase 4 — Project quality

| Area | Status |
| --- | --- |
| Quality report from sampled source | Slice, unverified. Heuristics for size, secrets-shaped strings, tests present, SQL patterns. |
| Confirmed vs probable findings | Partial. |
| Measured coverage or test execution | Correctly not invented. Detection only. |
| Adaptive modes | Slice via `AnalysisMode`. Criteria are coarse. |

### Phase 5 — Advanced AI

| Area | Status |
| --- | --- |
| Assistant | Partial. Retrieves the caller’s own `KnowledgeChunk` rows plus a prompt. Not a full RAG pipeline, no conversation delete, limited injection defense. |
| Interview pack and JD draft | Slice, unverified. |
| Recruitment analytics | Partial. Narrative summary from counts, not stage timing or volume series. |
| Notifications | Missing. |

### Phase 6 — Security, testing, reliability

Present: BCrypt, role checks on applications and jobs, upload name checks, audit log writes, AI daily cap, consent flag (defaults to **true**), H2 API test, Vitest helper.

Missing or weak: Flyway, Testcontainers, E2E, rate limiting (before this milestone), secret scanning, refresh rotation, account lockout persistence, production error sanitization (generic handler returned `ex.getMessage()`), authorization tests for cross-user access, JaCoCo.

### Phase 7 — Deployment

Present: Dockerfiles, Compose, CI workflow, actuator health, springdoc.

Missing: `package-lock.json` (CI `npm ci` would fail), cloud deployment, backups, load tests, migration-based schema. Maven is not installed globally on this machine.

## Known problems

1. `ddl-auto: update` can change production tables without a reviewed migration.
2. Default JWT secret and MySQL passwords in Compose are development defaults. They must be overridden outside local demo.
3. `GET /api/jobs/{id}` is public for every status, including drafts if a caller knows the id.
4. Application status changes are not a state machine.
5. AI consent defaults to granted.
6. Match percentages are shown without a documented formula.
7. GitHub analysis runs inside the HTTP request.
8. Generic exceptions echoed internal messages to clients.
9. CI frontend job expects a lockfile that was not committed.
10. Package rename to `com.proofnexa` is deferred. A mechanical rename touches every import and the persisted app identity without improving behavior.

## Prioritized tasks

1. **Auth completion (this session).** Password policy, reset tokens, login and reset rate limits, deactivation, logout audit, password visibility, sanitized unexpected errors.
2. **Candidate profile sections.** Education, experience, skills, certifications, projects, contact and preference fields, with ownership checks and tests.
3. **Job lifecycle and saved jobs.** Draft, publish, close, archive, preferred skills, salary, deadline, openings, bookmarks, server-side sort.
4. **Application state machine.** Spec statuses, actor on each event, withdraw rules, no duplicate active applications.
5. **Dashboards from authorized aggregates.** No hardcoded counts.
6. **Flyway baseline** taken from the current Hibernate schema, then `ddl-auto: validate` outside tests.
7. **Organizations and recruiter membership.**
8. **Async AI jobs** with explicit status, then deeper skill-proof and quality work.
9. **Notifications, RAG assistant hardening, interview practice persistence.**
10. **Testcontainers, Playwright journey, lockfile in CI, deployment docs.**

## Files for the auth milestone

- `backend/.../domain/User.java`, `PasswordResetToken.java`
- `backend/.../repo/PasswordResetTokenRepository.java`
- `backend/.../security/PasswordPolicy.java`, `LoginAttemptLimiter.java`, `AuthUser.java`, `JwtAuthFilter.java`
- `backend/.../service/AuthService.java`
- `backend/.../web/AuthController.java`
- `backend/.../dto/Dtos.java`
- `backend/.../config/ProoforaProperties.java`
- `backend/.../exception/ApiException.java`, `GlobalExceptionHandler.java`
- `backend/src/main/resources/application.yml`, `backend/src/test/resources/application.yml`
- `backend/src/test/java/com/proofora/AuthSecurityFlowTest.java`
- `frontend/src/components/PasswordField.jsx`, `AccountSecurity.jsx`
- `frontend/src/utils/passwordRules.js`, `passwordRules.test.js`
- `frontend/src/pages/Login.jsx`, `Register.jsx`, `ForgotPassword.jsx`, `ResetPassword.jsx`
- `frontend/src/App.jsx`, `AuthContext.jsx`, `styles.css`, `package.json`
- `README.md`, `ROADMAP.md`, `CHANGELOG.md`, `.env.example`

No database migration file yet. Hibernate `ddl-auto` creates `password_reset_tokens` in development and tests. `users.enabled` is nullable on purpose: null means enabled, so existing rows are not locked out when the column appears.

## Dependencies

No new libraries. Reset email delivery is not wired. Set `PROOFORA_EXPOSE_RESET_TOKEN=true` only on a local machine when you need the API to return the reset token. Leave it false anywhere else.

## Testing plan for this milestone

- `mvn -B test` — weak password, reset round-trip, reused token, deactivation, login rate limit, disabled JWT rejected.
- `npm test` — password rule helper and existing session helper. Script changed to `vitest run` so it exits.

## Risks

- In-memory rate limits reset on restart and are not shared across instances.
- Issued JWTs are not revoked on logout; deactivation is enforced by reloading the user on each request.
- Exposing reset tokens is a local debugging switch. Email delivery is still pending.
- `ddl-auto: update` remains until the Flyway milestone.

## Phase completion criteria

Phase 1 is **not** complete. Exit still requires profile sections, organization-aware recruiting, a real job lifecycle, a status state machine, and dashboards backed by aggregates, all on MySQL with authorization tests.

## Milestone completed

**Auth hardening** is implemented and covered by automated tests.

Verified on 2026-10-16:

- `mvn -B test` — BUILD SUCCESS, 8 tests, 0 failures (H2, not MySQL).
- `npm test` — 3 tests passed (`vitest run`).

Not verified: browser walkthrough, MySQL `ddl-auto` column add, live email, Docker Compose.

Next task: candidate profile sections (education, experience, skills, certifications, projects, contact and work preferences) with ownership checks and API tests.
