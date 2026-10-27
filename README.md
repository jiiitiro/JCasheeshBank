# JCasheesh! Bank — Spring Boot Application

A modern, full-stack Spring Boot banking application built with *Spring Data JPA*, *Spring Web*, and *Thymeleaf*, enhanced with advanced security features, asynchronous email verification, and a sleek, responsive *Tailwind CSS* glassmorphism interface.

📄 **Project Portfolio (PDF):** [docs/JCasheeshBank-Project-Portfolio.pdf](docs/JCasheeshBank-Project-Portfolio.pdf) — project description, database schema, source code highlights, and screenshots of every screen.

---

## 🚀 Key Features Implemented

### Modern UI/UX & Frontend Design
* **Glassmorphism Aesthetic**: Centered single-column layout featuring frosted glass effects (`backdrop-blur-2xl`), custom deep shadows, and a vibrant purple-to-indigo-to-blue gradient background.
* **Interactive Tab Switching**: Client-side switching between *Sign In* and *Register* views with dynamic header updates and automated server-side error redirection support (`showRegister` flag handling).
* **Smart Input Validation & Formatting**:
    * Mobile numbers lock in the Philippine standard `09` prefix with strict 11-digit numeric enforcement.
    * 4-digit PIN fields with secure masking and numeric-only constraints.
    * Real-time pattern validation for full names and email addresses.

### Express Send & Cash-In Workflows
* **Two-Step Cash-In Flow**: Amount input (₱5.00–₱10,000.00), a confirmation view (`/cash-in/confirm`), and a receipt page (`/cash-in/receipt/{id}`) with an auto-generated reference number, timestamp and updated balance.
* **Peer-to-Peer Express Send**: Receiver name masking for privacy, self-transfer prevention, balance validation, a confirmation preview (`/transfer/confirm`), and a printable receipt page (`/transfer/receipt/{id}`).
* **Refresh-Safe Receipts**: After a cash-in or transfer, the app redirects to the receipt page, so refreshing it never repeats the payment. Users can only open receipts for their own transactions.

### Secure Authentication & Account Security
* **BCrypt Hashing**: Encrypted PIN storage and validation.
* **Email Verification Flow**: Mandatory email verification upon registration; unverified accounts are blocked from logging in.
* **Brute-Force Protection**: Failed login attempts are counted per account in the database; the 3rd failure locks the account and emails a PIN-reset link.
* **Secure Recovery**: Time-sensitive token generation (15-minute expiry) for unlocking accounts and resetting PINs. The new PIN is validated on the server (exactly 4 digits).

### Security Hardening
* **CSRF Protection**: Spring Security adds a CSRF token to every form; requests without a valid token are rejected with 403.
* **Session Fixation Protection**: The session ID is rotated on every successful login.
* **Safe Concurrent Transfers**: Account rows are locked (`SELECT … FOR UPDATE`) during cash-in and transfers, so simultaneous transfers can't overdraw an account.
* **Server-Side Limits**: Cash-in limits and positive-amount checks are enforced in the service layer, not only in the browser.
* **No Internal Error Leaks**: Unexpected errors are logged on the server; users see a generic message.

See [SECURITY.md](SECURITY.md) for the full list of security fixes and what is still open.

### Asynchronous Email Infrastructure
* Non-blocking background email sending via Spring `@Async` and `JavaMailSender` (Gmail SMTP) for account verification and PIN-reset emails.

---

## 🛠 Technology Stack

* **Backend**: Java 17, Spring Boot 3.5.6, Spring Web, Spring Data JPA, Spring Security (CSRF + BCrypt), Spring Mail (`JavaMailSender`), Lombok
* **Frontend**: Thymeleaf, HTML5, Tailwind CSS (via CDN), Custom JavaScript
* **Database**: PostgreSQL 15
* **Containerization & Deployment**: Docker, Docker Compose, Railway Platform
* **Build Tool**: Maven 3.9

---

## 📁 Project Structure

```
src/main/java/com/jcashbank/
├── config/        DataInitializer (seed users), SecurityConfig (CSRF)
├── controller/    AuthController, DashboardController, CashInController, TransferController,
│                  SessionUserProvider (logged-in user lookup), GlobalExceptionHandler
├── exception/     BankingException
├── model/         User, Transaction, TransactionType
├── repository/    UserRepository, TransactionRepository
└── service/       UserService, TransactionService, EmailService,
                   UserValidationService, TransactionValidationService
src/main/resources/
├── templates/     Thymeleaf pages (login, dashboard, cash-in, transfer, receipts, PIN reset)
└── static/        CSS and images
```

---

## 🐳 Running with Docker Compose

The project includes a multi-stage `Dockerfile` and a `docker-compose.yml` that runs the app together with PostgreSQL 15.

1. Clone the repository and navigate to the project root directory.
2. Create a `.env` file in the project root with these variables (never commit it):
   ```
   POSTGRES_DB=jcasheesh_db
   POSTGRES_USER=postgres
   POSTGRES_PASSWORD=<your-password>
   MAIL_USERNAME=<gmail-address>
   MAIL_PASSWORD=<gmail-app-password>
   APP_BASE_URL=http://localhost:8081
   ```
3. Launch the application and PostgreSQL container together:
   ```bash
   docker compose up --build
   ```
4. Open http://localhost:8081.

To test from other devices on your LAN, or to export the images to another machine (e.g. Windows 11), see [docs/DOCKER-EXPORT.md](docs/DOCKER-EXPORT.md).

## 💻 Running Locally (IntelliJ)

1. Start only the database: `docker compose up -d postgres` (exposed on `localhost:5434`).
2. Run `JcashBankApplication` from IntelliJ. The app listens on port **8081**.

On first start with an empty database, `DataInitializer` creates three test accounts:

| Mobile | PIN | Starting balance |
|---|---|---|
| 09171234567 | 1234 | ₱0.00 |
| 09181234567 | 5678 | ₱0.00 |
| 09201234567 | 9999 | ₱1,000.00 |
