# Security Fixes: JCash Bank

This document records the security problems found in the JCash Bank app and how each was fixed. All changes were made on the `lombok` branch and merged into `main-dev`.

## Security fixes

### 1. A refreshed receipt page repeated the payment
- **Problem:** `/transfer/execute` and `/cash-in/execute` showed the receipt directly in response to the form submission. Refreshing the page sent the form again, so the money moved a second time.
- **Fix:** After the payment, the app now redirects to a separate receipt page (`GET /transfer/receipt/{id}`, `GET /cash-in/receipt/{id}`), so a refresh just reloads the receipt. The receipt page checks that the transaction belongs to the logged-in user, so other people's receipts can't be viewed by guessing an ID.
- **Files:** `TransferController`, `CashInController`, `TransactionService`, `TransactionRepository`
- **Commit:** `10251ca`

### 2. The cash-in limit could be bypassed
- **Problem:** The ₱5–₱10,000 limit was only checked on the confirm step. The final step read the amount from a hidden form field and only checked that it was above zero. Anyone could post any amount straight to `/cash-in/execute`.
- **Fix:** The limit is now enforced in the service layer (`TransactionValidationService.validateCashInAmount`), which every cash-in goes through. The confirm step uses the same check.
- **Commit:** `10251ca`

### 3. An old endpoint skipped the confirmation step
- **Problem:** An unused `POST /transfer` endpoint could still send money without going through the confirmation screen.
- **Fix:** Removed the endpoint.
- **Commit:** `10251ca`

### 4. The new PIN wasn't checked on the server
- **Problem:** Only the browser checked that a reset PIN was 4 digits. A direct request could set a PIN like `"abc"`, and that user could then never log in.
- **Fix:** `UserService.resetPin` now checks the PIN (`UserValidationService.validatePin`) before doing anything else, and the controller trims the input.
- **Commit:** `10251ca`

### 5. Session fixation
- **Problem:** The session ID stayed the same after login. An attacker who planted a session ID on a victim before login would share the victim's logged-in session.
- **Fix:** `AuthController.login()` now calls `request.changeSessionId()` after a successful login.
- **Commit:** `10251ca`

### 6. No CSRF protection
- **Problem:** Any website could make a logged-in user's browser submit a request, including a transfer.
- **Fix:** Added `spring-boot-starter-security` with a `SecurityConfig` that turns on CSRF protection. Login and session handling still work the app's own way. Thymeleaf adds the token to every form automatically, and a request without a valid token is rejected with 403.
- **Verified:** Logging in without the token was rejected with 403, and logging in with it succeeded.
- **Commit:** `10251ca`

### 7. Two transfers at the same moment could overdraw an account
- **Problem:** The transfer code read the balance, checked it and saved it without locking the account. Two transfers at the same moment could both pass the "enough balance" check.
- **Fix:** `TransactionService` now locks the user's database row (`SELECT … FOR UPDATE`) during cash-in and transfer. It also re-reads the balance at that moment, so it's never working from an old copy. In a transfer, both accounts are always locked in the same order (lowest ID first), so two people sending to each other at once can't block each other forever.
- **Commit:** `10251ca`

### 8. Internal error details shown to users
- **Problem:** A failed registration could show the raw database or mail error text.
- **Fix:** Expected validation errors are still shown as written. Anything else is logged on the server, and the user sees a generic message.
- **Verified:** A real database error during registration was logged, and the form showed only the generic message.
- **Commit:** `2c29c5d`

### 9. Fake messages could be put in the page through a link
- **Problem:** The login page and dashboard displayed whatever text was in the `?success=` part of the address. Someone could send a link like `/login?success=Call 0917… to verify your account`.
- **Fix:** Success messages are now passed between pages with Spring's flash attributes instead of the address, and the pages no longer read the message from the URL.
- **Commit:** `2c29c5d`

### 10. Zero or negative transfer amounts reached the confirm screen
- **Problem:** `/transfer/confirm` showed a confirmation for amounts like −₱500. The final step did reject them.
- **Fix:** The amount is now checked (`validatePositiveAmount`) before the confirmation screen.
- **Commit:** `2c29c5d`

## Other bug fixes (not security)

| Problem | Fix | Commit |
|---|---|---|
| `GET /register` crashed because the `register` template doesn't exist | Redirects to `/login`, where the register tab is | `10251ca` |
| A failed transfer could end on the generic error page instead of the form | The recipient lookup in the error path is wrapped; the user goes back to the transfer form | `2c29c5d` |
| An empty or non-numeric amount showed a raw 400 page | `GlobalExceptionHandler` shows a friendly message | `2c29c5d` |
| Registration errors appeared on the login tab | Uses `registerError` and reopens the register tab | `2c29c5d` |
| The dashboard loaded the full transaction history to show 10 | Fetches only the latest 10 | `2c29c5d` |
| Registration failed with a not-null error on `balance` | Restored the `BigDecimal.ZERO` default that was lost in the Lombok conversion | `02338e2` |
| The forgot-PIN email linked to port 8080 instead of 8081 | Changed the reset link to `http://localhost:8081` (tested and working) | `02637fa` |

## Still open
- **Weak fallback settings:** `application.properties` falls back to `postgres` and `dummy_key_for_dev` if the environment variables are missing.
- **Unused setting:** `resend.api.key` is still in `application.properties` but nothing reads it.
- **No automated tests:** the test dependency is in place, but none are written. Fixes 1, 2 and 7 are the most important to cover.
- **Locking cost:** the account lock also reloads, and probably locks, the user's transaction rows, which gets slower as history grows.
- **Hard-coded reset links:** both PIN-reset emails use `http://localhost:8081`, so they won't work on Railway or Render. The verification email already uses `app.base-url` (`APP_BASE_URL`); the reset links should use it too.
- **Not fully tested by hand:** login, CSRF, the seed data and the forgot-PIN flow have been checked against the running app. The transfer and receipt-refresh flows still need a manual test.
