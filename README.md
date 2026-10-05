# JCasheesh Bank — Spring Boot Application

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

The application is configured to use a *PostgreSQL* database for robust data persistence.

### Configuration (application.properties)

Make sure your src/main/resources/application.properties contains your PostgreSQL connection details:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/jcash_bank
spring.datasource.username=postgres
spring.datasource.password=********
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
