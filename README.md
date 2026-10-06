# JCasheesh! Bank — Spring Boot Application

A modern, full-stack Spring Boot banking application built with *Spring Data JPA*, *Spring Web*, and *Thymeleaf*, enhanced with advanced security features, asynchronous email verification, and a sleek, responsive *Tailwind CSS* glassmorphism interface.

---

## 🚀 Key Features Implemented

### Modern UI/UX & Frontend Design
* *Glassmorphism Aesthetic*: Centered single-column layout featuring frosted glass effects (backdrop-blur-2xl), custom deep shadows, and a vibrant purple-to-indigo-to-blue gradient background.
* *Interactive Tab Switching*: Seamless, client-side switching between *Sign In* and *Register* views with dynamic header updates and automated server-side error redirection support (showRegister flag handling).
* *Smart Input Validation & Formatting*:
  * Mobile numbers automatically lock in the Philippine standard 09 prefix with strict 11-digit numeric enforcement.
  * 4-digit PIN fields with secure masking and numeric-only constraints.
  * Real-time pattern validation for full names and email addresses.

### Secure Authentication & Account Security
* *BCrypt Hashing*: Robust, encrypted PIN storage and validation.
* *Email Verification Flow*: Mandatory email verification upon registration; unverified accounts are blocked from logging in.
* *Brute-Force Protection*: Tracks failed login attempts (up to 3 per session) triggering automatic account locking.
* *Secure Recovery*: Time-sensitive token generation (15-minute expiry) for unlocking accounts and resetting PINs.

### Asynchronous Email Infrastructure
* Non-blocking background email dispatching via CompletableFuture and JavaMailSender for account verification codes and security alerts.

### Core Banking Operations
* *Dashboard & Balance*: Real-time balance display post-authentication.
* *Cash-In*: Instant funds addition with positive amount validation.
* *Secure Fund Transfers*: Peer-to-peer transfers via receiver mobile number complete with receiver validation, self-transfer prevention, and sender balance verification.
* *Audit Traceability*: Comprehensive transaction records featuring auto-generated unique reference numbers (referenceNumber) via JPA @PrePersist hooks.

---

## 🛠️️ Technology Stack

* *Backend*: Java 17+, Spring Boot 3.5.6, Spring Web, Spring Data JPA, Spring Mail (JavaMailSender)
* *Frontend*: Thymeleaf, HTML5, Tailwind CSS (via CDN), Custom JavaScript
* *Database*: PostgreSQL
* *Build Tool*: Maven

## Run

1. Install Java 17+ and Maven.
2. Open a terminal in this project directory.
3. Run:
4. mvn spring-boot:run
5. Open http://localhost:8080

## Build a JAR

```bash
mvn clean package
java -jar target/jcash-bank-1.0.0.jar
```

## Database

The application is configured to use a *PostgreSQL* database for robust data persistence.

### Configuration (application.properties)

Make sure your src/main/resources/application.properties contains your PostgreSQL connection details:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/jcash_bank
spring.datasource.username=postgres
spring.datasource.password=********
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
