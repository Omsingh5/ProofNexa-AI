# Development checklist

Legend: `[x]` vertical slice present in this repo · `[~]` partial / follow-up · `[ ]` not started

Checked items are not a claim that the master specification is finished. See [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) for the gap analysis. Java packages remain `com.proofora`.

## Phase 1 — Core application

- [x] User registration, login, and role-based access (Candidate, Recruiter, Admin)
- [x] Password strength, reset tokens, login rate limit, deactivation, and logout audit
- [ ] Email verification and refresh-token rotation (no mail provider configured)
- [x] Job posting, editing, searching, filtering, and pagination
- [x] Job application submission and status tracking
- [x] Resume upload and candidate profiles
- [x] React dashboards for candidates, recruiters, and administrators
- [x] MySQL database integration using Hibernate / Spring Data JPA

## Phase 2 — Essential AI integration

- [x] AI resume extraction and technical skill identification
- [x] AI skill-gap analysis based on job descriptions
- [x] AI job recommendations and explainable skill matching
- [x] Store AI analysis results and allow candidates to correct extracted data

## Phase 3 — AI Skill Proof & Career Growth Engine

- [x] GitHub repository integration and project submission
- [x] Technology and framework detection from source code
- [x] Evidence-based skill analysis with linked code references
- [x] Identify missing or unconfirmed skills against job requirements
- [x] AI-generated personalized learning roadmaps
- [x] AI-generated practical technical challenges
- [x] Track completed challenges and candidate skill development

## Phase 4 — AI Skill Depth & Project Quality Analyzer

- [x] Analyze project architecture and code organization
- [x] Evaluate code maintainability using parsers and static-analysis heuristics
- [x] Detect selected security vulnerabilities and coding issues
- [x] Analyze test existence (coverage % when present is not executed)
- [x] Inspect database design and performance-related code patterns
- [x] Generate evidence-backed project improvement recommendations
- [x] Adaptive modes: Skill Development, Validation, Career Advancement

## Phase 5 — Advanced AI features

- [x] RAG-style career assistant over authorized knowledge chunks
- [x] AI interview question generator
- [x] AI job-description writing assistant
- [x] Candidate application feedback and progress timeline
- [x] AI-powered recruitment analytics summaries

## Phase 6 — Security, testing & reliability

- [x] Spring Security authorization and AI consent flag
- [x] Resume file type checks and path-safe storage
- [x] Input validation, API exceptions, audit logs
- [x] JUnit tests (H2)
- [x] Frontend Vitest for session helpers
- [x] AI daily rate limit, provider failure fallback, cost-oriented model default
- [x] Submitted code is reviewed as text only — never executed

Follow-ups: Testcontainers MySQL, full component/E2E suite, dedicated secret scanning.

## Phase 7 — Deployment & production quality

- [x] Docker and Docker Compose
- [x] GitHub Actions CI
- [ ] Cloud deployment (AWS or other) — config-ready, not provisioned
- [x] Database indexes on jobs + pagination
- [x] Actuator health + structured exception payloads
- [x] Swagger/OpenAPI + professional README
- [x] Architecture diagram for interviews (README)

Follow-ups: load testing, backups, live demo hosting.
