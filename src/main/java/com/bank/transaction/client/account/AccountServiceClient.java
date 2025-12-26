package com.bank.transaction.client.account;

import com.bank.transaction.client.account.dto.Account;
import com.bank.transaction.client.account.dto.BalanceValidationResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.math.BigDecimal;

@Path("/api/accounts")
@RegisterRestClient(configKey = "account-service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface AccountServiceClient {

    @GET
    @Path("/{accountNumber}")
    Account getAccount(@PathParam("accountId") String accountNumber,
                       @HeaderParam("Authorization") String authorization);

    @POST
    @Path("/{accountNumber}/validate-balance")
    BalanceValidationResponse validateBalance(@PathParam("accountNumber") String accountNumber,
                                              @QueryParam("amount") BigDecimal amount,
                                              @HeaderParam("Authorization") String authorization);

}
