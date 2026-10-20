package com.jcashbank.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 15)
    private String mobileNumber;

    @Column(nullable = false, length = 100)
    private String pin;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("transactionDate DESC")
    private List<Transaction> transactions = new ArrayList<>();

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column(nullable = false)
    private boolean accountLocked = false;

    private LocalDateTime lockTime; // Added missing lockTime field

    @Column(nullable = false)
    private boolean emailVerified;

    private String verificationToken;
    private LocalDateTime verificationTokenExpiry;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    public User(String mobileNumber, String pin, String fullName, BigDecimal balance, String email, boolean emailVerified) {
        this.mobileNumber = mobileNumber;
        this.pin = pin;
        this.fullName = fullName;
        this.balance = (balance != null) ? balance : BigDecimal.ZERO;
        this.email = email;
        this.emailVerified = emailVerified;
    }

    // Bidirectional Relationship Helpers
    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setUser(this);
    }

    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
        transaction.setUser(null);
    }
}
