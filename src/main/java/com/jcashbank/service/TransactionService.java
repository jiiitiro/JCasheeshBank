package com.jcashbank.service;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.Transaction;
import com.jcashbank.model.TransactionType;
import com.jcashbank.model.User;
import com.jcashbank.repository.TransactionRepository;
import com.jcashbank.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void cashIn(Long userId, BigDecimal amount) {
        validatePositiveAmount(amount);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BankingException("User account was not found."));

        user.setBalance(user.getBalance().add(amount));
        Transaction transaction = new Transaction(
                TransactionType.CASH_IN,
                amount,
                "Cash-in to wallet"
        );
        user.addTransaction(transaction);
        userRepository.save(user);
    }

    @Transactional
    public void transfer(Long senderId, String receiverMobileNumber, BigDecimal amount) {
        validatePositiveAmount(amount);
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new BankingException("Sender account was not found."));
        User receiver = userRepository.findByMobileNumber(receiverMobileNumber)
                .orElseThrow(() -> new BankingException("Receiver account was not found."));

        if (sender.getId().equals(receiver.getId())) {
            throw new BankingException("You cannot transfer money to your own account.");
        }
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new BankingException("Insufficient balance.");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        sender.addTransaction(new Transaction(
                TransactionType.TRANSFER_SENT,
                amount,
                "Transfer sent to " + receiver.getMobileNumber()
        ));
        receiver.addTransaction(new Transaction(
                TransactionType.TRANSFER_RECEIVED,
                amount,
                "Transfer received from " + sender.getMobileNumber()
        ));

        userRepository.save(sender);
        userRepository.save(receiver);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Amount must be greater than zero.");
        }
    }
}
