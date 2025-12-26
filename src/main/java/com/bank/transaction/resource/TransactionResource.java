package com.bank.transaction.resource;

import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.dto.TransferRequest;
import com.bank.transaction.service.TransactionService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import jakarta.annotation.PostConstruct;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Path("/api/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Transaction Resource", description = "Transaction operations")
public class TransactionResource {

    @Inject
    Logger logger;

    @Inject
    TransactionService transactionService;

    @Inject
    MeterRegistry meterRegistry;

    private Counter successfulTransfersCounter;
    private Counter failedTransfersCounter;
    private AtomicReference<BigDecimal> totalTransferredAmount = new AtomicReference<>(BigDecimal.ZERO);

    @PostConstruct
    void initMetrics() {
        successfulTransfersCounter = Counter.builder("transactions.successful.total")
                .description("Total number of successful transfers")
                .register(meterRegistry);
        
        failedTransfersCounter = Counter.builder("transactions.failed.total")
                .description("Total number of failed transfers")
                .register(meterRegistry);
        
        Gauge.builder("transactions.total.amount", totalTransferredAmount, ref -> ref.get().doubleValue())
                .description("Total amount transferred")
                .register(meterRegistry);
    }

    @POST
    @Path("/transfer")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    @Operation(summary = "Process money transfer between accounts")
    @APIResponse(
        responseCode = "201", 
        description = "Transfer processed successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Invalid transfer request")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "500", description = "Internal server error")
    public Response processTransfer(@Valid TransferRequest request) {
        logger.infof("Processing transfer request from %s to %s, amount: %s", 
                    request.sourceAccountId, request.targetAccountId, request.amount);
        
        try {
            TransactionResponse response = transactionService.processTransfer(request);
            
            // Update metrics
            successfulTransfersCounter.increment();
            totalTransferredAmount.updateAndGet(current -> current.add(request.amount));
            
            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (Exception e) {
            logger.errorf("Transfer failed: %s", e.getMessage());
            
            // Update metrics
            failedTransfersCounter.increment();
            
            throw e; // Let exception mappers handle the response
        }
    }

    @GET
    @Path("/{transactionId}")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    @Operation(summary = "Get transaction details by UUID")
    @APIResponse(
        responseCode = "200", 
        description = "Transaction found",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))
    )
    @APIResponse(responseCode = "404", description = "Transaction not found")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response getTransaction(@PathParam("transactionId") String transactionId) {
        logger.infof("Getting transaction: %s", transactionId);
        
        TransactionResponse response = transactionService.getTransaction(transactionId);
        return Response.ok(response).build();
    }

    @GET
    @Path("/account/{accountId}")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    @Operation(summary = "Get transaction history for an account")
    @APIResponse(
        responseCode = "200", 
        description = "Transaction history retrieved",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = TransactionResponse.class))
    )
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response getAccountTransactions(@PathParam("accountId") String accountId) {
        logger.infof("Getting transactions for account: %s", accountId);
        
        List<TransactionResponse> responses = transactionService.getAccountTransactions(accountId);
        return Response.ok(responses).build();
    }

}
