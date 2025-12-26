package com.bank.transaction.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String message) {
        super(message);
    }

    public TransactionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    @Provider
    public static class TransactionNotFoundExceptionMapper implements ExceptionMapper<TransactionNotFoundException> {
        @Override
        public Response toResponse(TransactionNotFoundException exception) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("TRANSACTION_NOT_FOUND", exception.getMessage()))
                    .build();
        }
    }

    public static class ErrorResponse {
        public String code;
        public String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
