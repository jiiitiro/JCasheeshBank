package com.jcashbank.service;

import com.jcashbank.exception.BankingException;
import com.jcashbank.model.Transaction;
import com.jcashbank.model.TransactionType;
import com.jcashbank.model.User;
import com.jcashbank.repository.TransactionRepository;
import com.jcashbank.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionValidationService transactionValidationService;

    @PersistenceContext
    private EntityManager entityManager;

    public TransactionService(UserRepository userRepository, TransactionRepository transactionRepository, TransactionValidationService transactionValidationService) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.transactionValidationService = transactionValidationService;
    }

    @Transactional
    public Transaction cashIn(Long userId, BigDecimal amount) {
        transactionValidationService.validateCashInAmount(amount);
        User user = lockUser(userId, "User account was not found.");

        user.setBalance(user.getBalance().add(amount));
        Transaction transaction = new Transaction(
                TransactionType.CASH_IN,
                amount,
                "Cash-in to wallet"
        );

        user.addTransaction(transaction);

        // User is managed, so the balance change flushes on commit. Persisting the transaction
        // directly (instead of userRepository.save, which merges and copies it) gives it an ID.
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction transfer(Long senderId, String receiverMobileNumber, BigDecimal amount) {
        transactionValidationService.validatePositiveAmount(amount);
        Long receiverId = userRepository.findByMobileNumber(receiverMobileNumber)
                .map(User::getId)
                .orElseThrow(() -> new BankingException("Receiver account was not found."));

        if (senderId.equals(receiverId)) {
            throw new BankingException("You cannot transfer money to your own account.");
        }

        // Lock both rows in ID order so two users sending to each other at once can't deadlock
        User sender;
        User receiver;
        if (senderId < receiverId) {
            sender = lockUser(senderId, "Sender account was not found.");
            receiver = lockUser(receiverId, "Receiver account was not found.");
        } else {
            receiver = lockUser(receiverId, "Receiver account was not found.");
            sender = lockUser(senderId, "Sender account was not found.");
        }

        // Balance is re-read under the lock, so concurrent transfers can't both pass this check
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

        transactionRepository.save(receivedTx);
        return transactionRepository.save(sentTx);
    }

    // Re-reads the row with SELECT ... FOR UPDATE. A refresh (rather than a locking query) matters
    // because open-in-view may already hold this user with a stale balance from the controller.
    private User lockUser(Long userId, String notFoundMessage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BankingException(notFoundMessage));
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        return user;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(Long userId) {
        return transactionRepository.findByUserIdOrderByTransactionDateDesc(userId);
    }

    // Dashboard "Recent Activity" only shows the latest 10, so don't load the full history
    @Transactional(readOnly = true)
    public List<Transaction> getRecentTransactions(Long userId) {
        return transactionRepository.findTop10ByUserIdOrderByTransactionDateDesc(userId);
    }

    // Only returns transactions owned by the user, so receipts can't be viewed by ID guessing
    @Transactional(readOnly = true)
    public Transaction getUserTransaction(Long userId, Long transactionId, TransactionType type) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .filter(tx -> tx.getType() == type)
                .orElseThrow(() -> new BankingException("Transaction not found."));
    }

    // The receiver's record shares the sender's reference number with a "-R" suffix
    @Transactional(readOnly = true)
    public String getTransferRecipientMobile(Transaction sentTx) {
        return transactionRepository.findByReferenceNumber(sentTx.getReferenceNumber() + "-R")
                .map(tx -> tx.getUser().getMobileNumber())
                .orElseThrow(() -> new BankingException("Transfer recipient was not found."));
    }


}
