# JCash Bank — Spring Boot Application

A Spring Boot + Spring Data JPA + Spring Web + Thymeleaf implementation of the JCash Banking App requirements in the Java NC III assessment PDF.

## Requirements implemented

- User model with ID, mobile number, PIN, full name, balance, and transaction list
- Transaction model with type, amount, details, and date/time
- JPA persistence
- Login with mobile number and PIN
- Maximum 3 failed login attempts per session
- Balance display after login
- Cash-in with positive amount validation
- Transfer by receiver mobile number
- Receiver validation
- Sender balance validation
- Transfer to self prevented
- Sender and receiver transaction records
- Transaction history
- Logout
- Thymeleaf UI
- Demo seed users

## Technology

- Java 17+
- Spring Boot 3.5.6
- Spring Web
- Spring Data JPA
- Thymeleaf
- H2 Database
- Maven

## Run

1. Install Java 17+ and Maven.
2. Open a terminal in this project directory.
3. Run:

```bash
mvn spring-boot:run
```

4. Open http://localhost:8080

## Demo accounts

| User | Mobile | PIN | Starting Balance |
|---|---|---|---:|
| Juan Dela Cruz | 09171234567 | 1234 | 5000.00 |
| Maria Santos | 09181234567 | 5678 | 2500.00 |
| Pedro Reyes | 09201234567 | 9999 | 1000.00 |

## Build a JAR

```bash
mvn clean package
java -jar target/jcash-bank-1.0.0.jar
```

## Database

The default configuration uses a file-based H2 database at `./data/jcashdb` so data survives application restarts.

H2 console:

http://localhost:8080/h2-console

JDBC URL:

`jdbc:h2:file:./data/jcashdb`

User: `sa`

Password: blank

## Important assessment note

The PDF asks for a PIN attribute but does not specify password hashing. This implementation keeps the PIN simple for assessment/demo purposes. For a production banking system, use Spring Security and a password encoder such as BCrypt rather than storing PINs as plain text.
