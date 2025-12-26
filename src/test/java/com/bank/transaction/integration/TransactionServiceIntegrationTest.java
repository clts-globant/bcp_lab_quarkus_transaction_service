package com.bank.transaction.integration;

import com.bank.transaction.client.account.AccountServiceClient;
import com.bank.transaction.client.account.dto.Account;
import com.bank.transaction.client.account.dto.BalanceValidationResponse;
import com.bank.transaction.client.customer.CustomerServiceClient;
import com.bank.transaction.client.customer.dto.CustomerValidationResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import java.math.BigDecimal;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Transaction Service endpoints.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TransactionServiceIntegrationTest {

    public static final String SOURCE_ACCOUNT_NUMBER = "12345";
    public static final String TARGET_ACCOUNT_NUMBER = "67890";
    public static final BigDecimal AMOUNT = new BigDecimal("100.50");
    public static final Long CUSTOMER_ID = 123L;
    public static final String TOKEN = "Bearer xyz";
    public static final String TOKEN_2 = "Bearer valid-token";

    @InjectMock
    @RestClient
    AccountServiceClient accountServiceClientMock;

    @InjectMock
    @RestClient // Identify the bean to be mocked
    CustomerServiceClient customerServiceClientMock;

    private static String transactionId;

    @BeforeEach
    public void setMocks(){
        // Account balance validation mock
        BalanceValidationResponse sourceAccountMockResponse = new BalanceValidationResponse();
        sourceAccountMockResponse.hasBalance = true;
        BalanceValidationResponse targetAccountMockResponse = new BalanceValidationResponse();
        targetAccountMockResponse.hasBalance = true;

        when(accountServiceClientMock.validateBalance(eq(SOURCE_ACCOUNT_NUMBER), eq(AMOUNT), anyString()))
                .thenReturn(sourceAccountMockResponse);
        when(accountServiceClientMock.validateBalance(eq(TARGET_ACCOUNT_NUMBER), eq(AMOUNT), anyString()))
                .thenReturn(targetAccountMockResponse);

        // Account query mock
        Account sourceAccount = new Account();
        sourceAccount.customerId = CUSTOMER_ID;
        sourceAccount.status = "ACTIVE";
        Account targetAccount = new Account();
        targetAccount.customerId = CUSTOMER_ID;
        targetAccount.status = "ACTIVE";
        when(accountServiceClientMock.getAccount(eq(SOURCE_ACCOUNT_NUMBER), anyString()))
                .thenReturn(sourceAccount);
        when(accountServiceClientMock.getAccount(eq(TARGET_ACCOUNT_NUMBER), anyString()))
                .thenReturn(targetAccount);

        // Customer status validation mock
        CustomerValidationResponse customerMockResponse = new CustomerValidationResponse();
        customerMockResponse.valid = true;

        when(customerServiceClientMock.validateCustomer(eq(CUSTOMER_ID), any(String.class)))
                .thenReturn(customerMockResponse);
    }

    @Test
    @Order(1)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testProcessTransfer() {
        String transferRequest = """
            {
                "sourceAccountId": "12345",
                "targetAccountId": "67890", 
                "amount": 100.50,
                "description": "Integration test transfer"
            }
            """;

        transactionId = given()
            .contentType(ContentType.JSON)
            .body(transferRequest)
            .when()
            .post("/api/transactions/transfer")
            .then()
            .statusCode(201)
            .body("transactionId", notNullValue())
            .body("sourceAccountId", is("12345"))
            .body("targetAccountId", is("67890"))
            .body("amount", is(100.50f))
            .body("status", is("COMPLETED"))
            .body("description", is("Integration test transfer"))
            .extract()
            .path("transactionId");
    }

    @Test
    @Order(2)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testGetTransaction() {
        given()
            .when()
            .get("/api/transactions/" + transactionId)
            .then()
            .statusCode(200)
            .body("transactionId", is(transactionId))
            .body("status", is("COMPLETED"))
            .body("amount", is(100.50f));
    }

    @Test
    @Order(3)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testProcessTransfer_SameAccount() {
        String transferRequest = """
            {
                "sourceAccountId": "12345",
                "targetAccountId": "12345", 
                "amount": 50.00,
                "description": "Same account transfer"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(transferRequest)
            .when()
            .post("/api/transactions/transfer")
            .then()
            .statusCode(400); // Bad Request - Same account
    }

    @Test
    @Order(4)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testProcessTransfer_InvalidAmount() {
        String transferRequest = """
            {
                "sourceAccountId": "12345",
                "targetAccountId": "67890", 
                "amount": -100.00,
                "description": "Negative amount transfer"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(transferRequest)
            .when()
            .post("/api/transactions/transfer")
            .then()
            .statusCode(400); // Bad Request - Invalid amount
    }

    @Test
    @Order(5)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testProcessTransfer_InsufficientFunds() {
        String transferRequest = """
            {
                "sourceAccountId": "12345",
                "targetAccountId": "67890", 
                "amount": 999999.00,
                "description": "Insufficient funds transfer"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(transferRequest)
            .when()
            .post("/api/transactions/transfer")
            .then()
            .statusCode(400); // Bad Request - Insufficient funds
    }

    @Test
    @Order(6)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testGetAccountTransactions() {
        given()
            .when()
            .get("/api/transactions/account/12345")
            .then()
            .statusCode(200);
    }

    @Test
    @Order(7)
    @TestSecurity(user = "user", roles = {"ROLE_USER"})
    public void testGetTransaction_NotFound() {
        given()
            .when()
            .get("/api/transactions/NON-EXISTENT-ID")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(8)
    public void testTransferWithoutAuthentication_ShouldFail() {
        String transferRequest = """
            {
                "sourceAccountId": "12345",
                "targetAccountId": "67890", 
                "amount": 100.00,
                "description": "Unauthorized transfer"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(transferRequest)
            .when()
            .post("/api/transactions/transfer")
            .then()
            .statusCode(401); // Unauthorized
    }

    @Test
    public void testHealthEndpoint() {
        given()
            .when()
            .get("/q/health")
            .then()
            .statusCode(200)
            .body("status", is("UP"));
    }

    @Test
    public void testMetricsEndpoint() {
        given()
            .when()
            .get("/q/metrics")
            .then()
            .statusCode(200);
    }
}
