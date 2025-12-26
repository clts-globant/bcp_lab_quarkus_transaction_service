package com.bank.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class TransferRequest {

    @NotBlank(message = "Source account ID is required")
    public String sourceAccountId;

    @NotBlank(message = "Target account ID is required")
    public String targetAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    public BigDecimal amount;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    public String description;

    public TransferRequest() {}

    public TransferRequest(String sourceAccountId, String targetAccountId, BigDecimal amount, String description) {
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.description = description;
    }
}
