# JCasheesh Bank — Spring Boot Application

A Spring Boot + Spring Data JPA + Spring Web + Thymeleaf implementation of the JCash Banking App requirements, enhanced with advanced security features, email verification, asynchronous notifications, and unique transaction reference tracking.

## Requirements Implemented

- *User Model & Persistence*: Manages user details (ID, mobile number, encrypted PIN, full name, balance, verification status, and transaction list) using Spring Data JPA and PostgreSQL.
- *Secure Authentication*:
  - BCrypt password encoder for secure PIN storage and validation.
  - Mandatory email verification flow upon registration (unverified users are blocked at authentication).
  - Maximum 3 failed login attempts per session.
- *Account Locking & PIN Reset*:
  - Automatic account locking upon reaching 3 failed login attempts.
  - Secure, time-sensitive token generation (15-minute expiry) for unlocking and resetting PINs.
- *Asynchronous Email Infrastructure*: Non-blocking background email dispatching via CompletableFuture and JavaMailSender for account verification and security alerts.
- *Core Banking Operations*:
  - Balance display after login.
  - Cash-in with positive amount validation.
  - Transfer by receiver mobile number with receiver validation, self-transfer prevention, and sender balance verification.
  - Comprehensive transaction records featuring **auto-generated unique reference numbers (referenceNumber)** via JPA @PrePersist hooks for full audit traceability.
- *Session & UI Management*: Secure login/logout states, responsive Thymeleaf-powered UI with Tailwind CSS, and demo seed users.

## Technology

- Java 17+
- Spring Boot 3.5.6
- Spring Web
- Spring Data JPA
- Spring Mail (JavaMailSender)
- Thymeleaf
- PostgreSQL Database
- Maven

## Run

1. Install Java 17+ and Maven.
2. Open a terminal in this project directory.
3. Run:

```bash
mvn spring-boot:run
```

4. Open http://localhost:8080

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
