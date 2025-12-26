package com.bank.transaction.producer;

import com.bank.transaction.dto.TransactionEvent;
import com.bank.transaction.entity.Transaction;
import com.bank.transaction.entity.TransactionStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class TransactionEventProducer {

    @Inject
    Logger logger;

    @Inject
    @Channel("transactions-completed")
    Emitter<TransactionEvent> completedTransactionsEmitter;

    @Inject
    @Channel("transactions-failed")
    Emitter<TransactionEvent> failedTransactionsEmitter;

    public void publishTransactionCompleted(Transaction transaction) {
        TransactionEvent event = new TransactionEvent(
            transaction.transactionId,
            transaction.sourceAccountId,
            transaction.targetAccountId,
            transaction.amount,
            transaction.timestamp,
            TransactionStatus.COMPLETED,
            transaction.description
        );
        
        logger.infof("Publishing transaction completed event for transaction: %s", transaction.transactionId);
        completedTransactionsEmitter.send(event);
    }

    public void publishTransactionFailed(Transaction transaction, String errorMessage) {
        TransactionEvent event = new TransactionEvent(
            transaction.transactionId,
            transaction.sourceAccountId,
            transaction.targetAccountId,
            transaction.amount,
            transaction.timestamp,
            TransactionStatus.FAILED,
            transaction.description,
            errorMessage
        );
        
        logger.infof("Publishing transaction failed event for transaction: %s, error: %s", 
                    transaction.transactionId, errorMessage);
        failedTransactionsEmitter.send(event);
    }
}
