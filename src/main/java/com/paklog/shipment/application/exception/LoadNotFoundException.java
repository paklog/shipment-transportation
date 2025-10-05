package com.paklog.shipment.application.exception;

public class LoadNotFoundException extends RuntimeException {
    public LoadNotFoundException(String message) {
        super(message);
    }
}
