package com.bank.transaction.dto;

import com.bank.transaction.entity.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {

    public Long id;
    public String transactionId;
    public String sourceAccountId;
    public String targetAccountId;
    public BigDecimal amount;
    public LocalDateTime timestamp;
    public TransactionStatus status;
    public String description;

    public TransactionResponse() {}

    public TransactionResponse(Long id, String transactionId, String sourceAccountId,
                              String targetAccountId, BigDecimal amount, LocalDateTime timestamp,
                              TransactionStatus status, String description) {
        this.id = id;
        this.transactionId = transactionId;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
        this.description = description;
    }
}
