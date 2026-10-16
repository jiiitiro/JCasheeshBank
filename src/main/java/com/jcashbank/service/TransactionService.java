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
    private final TransactionValidationService transactionValidationService;

    public TransactionService(UserRepository userRepository, TransactionRepository transactionRepository, TransactionValidationService transactionValidationService) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.transactionValidationService = transactionValidationService;
    }

    @Transactional
    public Transaction cashIn(Long userId, BigDecimal amount) {
        transactionValidationService.validatePositiveAmount(amount);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BankingException("User account was not found."));

        user.setBalance(user.getBalance().add(amount));
        Transaction transaction = new Transaction(
                TransactionType.CASH_IN,
                amount,
                "Cash-in to wallet"
        );

        transaction.prePersist();

        user.addTransaction(transaction);
        userRepository.save(user);

        return transaction;
    }

    @Transactional
    public Transaction transfer(Long senderId, String receiverMobileNumber, BigDecimal amount) {
        transactionValidationService.validatePositiveAmount(amount);
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

        // Base GCash-style digits
        String baseRefNo = String.format("%04d %03d %06d",
                (int)(Math.random() * 9000) + 1000,
                (int)(Math.random() * 900) + 100,
                (int)(Math.random() * 900000) + 100000);

        // 1. Sender transaction record
        Transaction sentTx = new Transaction(
                TransactionType.TRANSFER_SENT,
                amount,
                "Transfer sent to " + receiver.getMobileNumber()
        );
        sentTx.setReferenceNumber(baseRefNo); // Unique for sender

        // 2. Receiver transaction record (distinct reference_number)
        Transaction receivedTx = new Transaction(
                TransactionType.TRANSFER_RECEIVED,
                amount,
                "Transfer received from " + sender.getMobileNumber()
        );
        receivedTx.setReferenceNumber(baseRefNo + "-R"); // Appended suffix keeps unique constraint happy

        sender.addTransaction(sentTx);
        receiver.addTransaction(receivedTx);

        userRepository.save(sender);
        userRepository.save(receiver);

        return sentTx;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
    }


}
