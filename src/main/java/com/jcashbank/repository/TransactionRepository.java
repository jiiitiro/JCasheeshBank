package com.jcashbank.repository;

import com.jcashbank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserIdOrderByTransactionDateDesc(Long userId);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    Optional<Transaction> findByReferenceNumber(String referenceNumber);
}
