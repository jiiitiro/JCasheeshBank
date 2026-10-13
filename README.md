# JCasheesh! Bank — Spring Boot Application

A modern, full-stack Spring Boot banking application built with *Spring Data JPA*, *Spring Web*, and *Thymeleaf*, enhanced with advanced security features, asynchronous email verification, and a sleek, responsive *Tailwind CSS* glassmorphism interface.

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
* **Two-Step Cash-In Flow**: Includes amount input, a dedicated confirmation view (`/cash-in/confirm`), and a digital receipt view (`/cash-in-receipt`) complete with auto-generated reference numbers (`referenceNumber`), timestamps (`transactionDate`), and updated balance displays.
* **Peer-to-Peer Express Send**: Receiver name masking for privacy, self-transfer prevention, balance validation, confirmation preview (`confirm-send`), and a downloadable/printable receipt (`receipt`).

### Secure Authentication & Account Security
* **BCrypt Hashing**: Encrypted PIN storage and validation.
* **Email Verification Flow**: Mandatory email verification upon registration; unverified accounts are blocked from logging in.
* **Brute-Force Protection**: Tracks failed login attempts (up to 3 per session) triggering automatic account locking.
* **Secure Recovery**: Time-sensitive token generation (15-minute expiry) for unlocking accounts and resetting PINs.

### Asynchronous Email Infrastructure
* Non-blocking background email dispatching via `CompletableFuture` and `JavaMailSender` for account verification codes, password resets, and transaction alerts.

---

## 🛠 Technology Stack

* **Backend**: Java 17, Spring Boot 3.5.6, Spring Web, Spring Data JPA, Spring Mail (`JavaMailSender`)
* **Frontend**: Thymeleaf, HTML5, Tailwind CSS (via CDN), Custom JavaScript
* **Database**: PostgreSQL 15
* **Containerization & Deployment**: Docker, Docker Compose, Railway Platform
* **Build Tool**: Maven 3.9

---

## 🐳 Running with Docker Compose

The project includes a multi-stage `Dockerfile` and `docker-compose.yml` pre-configured with PostgreSQL health checks.

1. Clone the repository and navigate to the project root directory.
2. Launch the application and PostgreSQL container together:
   ```bash
   docker compose up --build
