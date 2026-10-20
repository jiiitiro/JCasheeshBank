package com.jcashbank.service;

import com.jcashbank.exception.BankingException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class TransactionValidationService {
    private static final BigDecimal CASH_IN_MIN = new BigDecimal("5.00");
    private static final BigDecimal CASH_IN_MAX = new BigDecimal("10000.00");

    public void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Amount must be greater than zero.");
        }
    }

    public void validateCashInAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(CASH_IN_MIN) < 0 || amount.compareTo(CASH_IN_MAX) > 0) {
            throw new BankingException("Cash in amount must be between ₱5.00 and ₱10,000.00.");
        }
    }
}
