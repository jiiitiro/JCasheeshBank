package com.jcashbank.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
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

    public Transaction(TransactionType type, BigDecimal amount, String details, LocalDateTime transactionDate, User user, String referenceNumber, LocalDateTime dateTime) {
        this.type = type;
        this.amount = amount;
        this.details = details;
        this.transactionDate = transactionDate;
        this.user = user;
        this.referenceNumber = referenceNumber;
        this.dateTime = dateTime;
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
            this.referenceNumber = generateFormattedRefNo();
        }
        if (this.transactionDate == null) {
            this.transactionDate = LocalDateTime.now();
        }
        if (this.dateTime == null) {
            this.dateTime = LocalDateTime.now();
        }
    }

    private String generateFormattedRefNo() {
        return String.format("%04d %03d %06d",
                (int)(Math.random() * 9000) + 1000,
                (int)(Math.random() * 900) + 100,
                (int)(Math.random() * 900000) + 100000);
    }

}
