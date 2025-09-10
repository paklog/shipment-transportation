package com.paklog.shipment.adapter.fedex.exception;

public class FedExAuthenticationException extends RuntimeException {
    public FedExAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public FedExAuthenticationException(String message) {
        super(message);
    }
}
