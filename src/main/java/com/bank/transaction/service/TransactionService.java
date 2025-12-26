package com.bank.transaction.service;

import com.bank.transaction.client.account.dto.Account;
import com.bank.transaction.client.account.dto.BalanceValidationResponse;
import com.bank.transaction.client.customer.dto.CustomerValidationResponse;
import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.dto.TransferRequest;
import com.bank.transaction.entity.Transaction;
import com.bank.transaction.entity.TransactionStatus;
import com.bank.transaction.exception.InvalidTransactionException;
import com.bank.transaction.exception.TransactionNotFoundException;
import com.bank.transaction.client.account.AccountServiceClient;
import com.bank.transaction.client.customer.CustomerServiceClient;
import com.bank.transaction.producer.TransactionEventProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class TransactionService {

    @Inject
    Logger logger;

    @Inject
    @RestClient
    AccountServiceClient accountServiceClient;

    @Inject
    @RestClient
    CustomerServiceClient customerServiceClient;

    @Inject
    TransactionEventProducer eventProducer;

    @Inject
    JsonWebToken jwt;

    @Transactional
    public TransactionResponse processTransfer(TransferRequest request) {
        logger.infof("Processing transfer from %s to %s, amount: %s", 
                    request.sourceAccountId, request.targetAccountId, request.amount);

        String transactionId = UUID.randomUUID().toString();
        String authHeader = "Bearer " + jwt.getRawToken();

        validateTransferRequest(request);

        Account sourceAccount = validateAccount(request.sourceAccountId, authHeader);
        Account targetAccount = validateAccount(request.targetAccountId, authHeader);

        // capacity not available in customer service, TBA
        //validateCustomerOwnership(sourceAccount.customerId, authHeader);

        validateSufficientBalance(request.sourceAccountId, request.amount, authHeader);

        Transaction transaction = new Transaction(
            transactionId,
            request.sourceAccountId,
            request.targetAccountId,
            request.amount,
            request.description
        );

        try {
            transaction.persist();

            transaction.status = TransactionStatus.COMPLETED;
            transaction.persist();

            eventProducer.publishTransactionCompleted(transaction);

            logger.infof("Transaction completed successfully: %s", transactionId);
            return mapToResponse(transaction);

        } catch (Exception e) {
            logger.errorf("Transaction failed: %s, error: %s", transactionId, e.getMessage());
            
            transaction.status = TransactionStatus.FAILED;
            transaction.persist();

            eventProducer.publishTransactionFailed(transaction, e.getMessage());

            throw new InvalidTransactionException("Transaction failed: " + e.getMessage(), e);
        }
    }

    public TransactionResponse getTransaction(String transactionId) {
        logger.infof("Getting transaction: %s", transactionId);
        
        Transaction transaction = Transaction.findByTransactionId(transactionId);
        if (transaction == null) {
            throw new TransactionNotFoundException("Transaction not found: " + transactionId);
        }
        
        return mapToResponse(transaction);
    }

    public List<TransactionResponse> getAccountTransactions(String accountId) {
        logger.infof("Getting transactions for account: %s", accountId);
        
        List<Transaction> transactions = Transaction.findByAccountId(accountId);
        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request.sourceAccountId.equals(request.targetAccountId)) {
            throw new InvalidTransactionException("Source and target accounts cannot be the same");
        }
        
        if (request.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero");
        }
    }

    private Account validateAccount(String accountId, String authHeader) {
        try {
            Account account = accountServiceClient.getAccount(accountId, authHeader);
            
            if (!"ACTIVE".equals(account.status)) {
                throw new InvalidTransactionException("Account is not active: " + accountId);
            }
            
            return account;
        } catch (Exception e) {
            throw new InvalidTransactionException("Account validation failed for: " + accountId, e);
        }
    }

    private void validateCustomerOwnership(Long customerId, String authHeader) {
        try {
            CustomerValidationResponse validation =
                customerServiceClient.validateCustomer(customerId, authHeader);
            
            if (!validation.valid) {
                throw new InvalidTransactionException("Source account doesn't belong to customer: " + customerId);
            }
        } catch (Exception e) {
            throw new InvalidTransactionException("Customer ownership validation failed", e);
        }
    }

    private void validateSufficientBalance(String accountId, BigDecimal amount, String authHeader) {
        try {
            BalanceValidationResponse validation =
                accountServiceClient.validateBalance(accountId, amount, authHeader);
            
            if (!validation.hasBalance) {
                throw new InvalidTransactionException("Insufficient funds in account: " + accountId);
            }
        } catch (Exception e) {
            throw new InvalidTransactionException("Balance validation failed for account: " + accountId, e);
        }
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
            transaction.id,
            transaction.transactionId,
            transaction.sourceAccountId,
            transaction.targetAccountId,
            transaction.amount,
            transaction.timestamp,
            transaction.status,
            transaction.description
        );
    }
}
