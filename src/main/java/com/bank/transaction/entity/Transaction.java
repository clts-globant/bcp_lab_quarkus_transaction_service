package com.bank.transaction.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "transactions")
public class Transaction extends PanacheEntity {

    @NotNull
    @Column(name = "transaction_id", unique = true, nullable = false)
    public String transactionId;

    @NotNull
    @Column(name = "source_account_id", nullable = false)
    public String sourceAccountId;

    @NotNull
    @Column(name = "target_account_id", nullable = false)
    public String targetAccountId;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Column(precision = 19, scale = 2, nullable = false)
    public BigDecimal amount;

    @NotNull
    @Column(nullable = false)
    public LocalDateTime timestamp;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TransactionStatus status;

    @Size(max = 255)
    @Column(length = 255)
    public String description;

    public Transaction() {
        this.timestamp = LocalDateTime.now();
        this.status = TransactionStatus.PENDING;
    }

    public Transaction(String transactionId, String sourceAccountId, String targetAccountId, BigDecimal amount, String description) {
        this();
        this.transactionId = transactionId;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.description = description;
    }

    public static Transaction findByTransactionId(String transactionId) {
        return find("transactionId", transactionId).firstResult();
    }

    public static List<Transaction> findByAccountId(String accountId) {
        return find("sourceAccountId = ?1 or targetAccountId = ?1 order by timestamp desc", accountId).list();
    }
}
