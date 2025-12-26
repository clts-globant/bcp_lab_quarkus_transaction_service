package com.bank.transaction.client.customer;

import com.bank.transaction.client.customer.dto.Customer;
import com.bank.transaction.client.customer.dto.CustomerValidationResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/customers")
@RegisterRestClient(configKey = "customer-service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface CustomerServiceClient {

    @GET
    @Path("/{id}")
    Customer getCustomer(@PathParam("id") Long id,
                         @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{id}/validate")
    CustomerValidationResponse validateCustomer(@PathParam("id") Long id,
                                                @HeaderParam("Authorization") String authorization);

}
