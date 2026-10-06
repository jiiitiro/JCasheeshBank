package com.jcashbank.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String details;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String referenceNumber;

    private LocalDateTime dateTime;

    public Transaction() {
    }

    public Transaction(TransactionType type, BigDecimal amount, String details) {
        this.type = type;
        this.amount = amount;
        this.details = details;
        this.transactionDate = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.referenceNumber == null || this.referenceNumber.isEmpty()) {
            this.referenceNumber = UUID.randomUUID().toString().substring(0, 13);
        }
        if (this.dateTime == null) {
            this.dateTime = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getReferenceNumber() {return referenceNumber;}
    public void setReferenceNumber(String referenceNumber) {this.referenceNumber = referenceNumber;}
    public LocalDateTime getDateTime() {return dateTime;}
    public void setDateTime(LocalDateTime dateTime) {this.dateTime = dateTime;}
}
