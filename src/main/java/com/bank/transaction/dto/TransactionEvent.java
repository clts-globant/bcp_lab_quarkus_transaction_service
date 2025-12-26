package com.bank.transaction.dto;

import com.bank.transaction.entity.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionEvent {

    public String transactionId;
    public String sourceAccountId;
    public String targetAccountId;
    public BigDecimal amount;
    public LocalDateTime timestamp;
    public TransactionStatus status;
    public String description;
    public String errorMessage;

    public TransactionEvent() {}

    public TransactionEvent(String transactionId, String sourceAccountId, String targetAccountId,
                           BigDecimal amount, LocalDateTime timestamp, TransactionStatus status,
                           String description) {
        this.transactionId = transactionId;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
        this.description = description;
    }

    public TransactionEvent(String transactionId, String sourceAccountId, String targetAccountId,
                           BigDecimal amount, LocalDateTime timestamp, TransactionStatus status,
                           String description, String errorMessage) {
        this(transactionId, sourceAccountId, targetAccountId, amount, timestamp, status, description);
        this.errorMessage = errorMessage;
    }
}
