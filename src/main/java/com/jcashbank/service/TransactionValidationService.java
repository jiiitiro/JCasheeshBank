package com.jcashbank.service;

import com.jcashbank.exception.BankingException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class TransactionValidationService {

    public void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Amount must be greater than zero.");
        }
    }
}
