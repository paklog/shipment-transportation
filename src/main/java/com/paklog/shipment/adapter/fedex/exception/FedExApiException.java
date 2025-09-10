package com.paklog.shipment.adapter.fedex.exception;

public class FedExApiException extends RuntimeException {
    public FedExApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public FedExApiException(String message) {
        super(message);
    }
}
