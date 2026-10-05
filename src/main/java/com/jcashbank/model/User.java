package com.jcashbank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
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
    private int failedLoginAttempts;

    @Column(nullable = false)
    private boolean accountLocked;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    public User() {}

    public User(String mobileNumber, String pin, String fullName, String email) {
        this.mobileNumber = mobileNumber;
        this.pin = pin;
        this.fullName = fullName;
        this.balance = BigDecimal.ZERO;
        this.email = email;
        this.accountLocked = false;
        this.failedLoginAttempts = 0;
    }

    public User(String mobileNumber, String pin, String fullName, BigDecimal balance, String email) {
        this.mobileNumber = mobileNumber;
        this.pin = pin;
        this.fullName = fullName;
        this.balance = balance == null ? BigDecimal.ZERO : balance;
        this.email = email;
        this.accountLocked = false;
        this.failedLoginAttempts = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    public String getEmail() {return email;}
    public void setEmail(String email) {this.email = email;}
    public int getFailedLoginAttempts() {return failedLoginAttempts;}
    public void setFailedLoginAttempts(int failedLoginAttempts) {this.failedLoginAttempts = failedLoginAttempts;}
    public boolean isAccountLocked() {return accountLocked;}
    public void setAccountLocked(boolean accountLocked) {this.accountLocked = accountLocked;}
    public String getResetToken() {return resetToken;}
    public void setResetToken(String resetToken) {this.resetToken = resetToken;}
    public LocalDateTime getResetTokenExpiry() {return resetTokenExpiry;}

    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        transaction.setUser(this);
    }

    public void removeTransaction(Transaction transaction) {
        transactions.remove(transaction);
        transaction.setUser(null);
    }

    public void setLockTime(Object o) {
    }
}
