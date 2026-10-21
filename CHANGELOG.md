# Changelog

## 2026-10-16

- Brought the existing Proofora vertical slice into this workspace and branded the product **ProofNexa AI** in the UI and docs. Java package `com.proofora` and database name `proofora` are unchanged so existing data and imports keep working.
- Added password strength checks, single-use hashed reset tokens, login and reset rate limits, account deactivation, and a logout audit event.
- Stopped returning raw exception messages for unexpected server errors.
- Frontend tests now exit (`vitest run`) instead of staying in watch mode.

Email delivery for password reset is not implemented. `PROOFORA_EXPOSE_RESET_TOKEN` must stay false outside local debugging.
