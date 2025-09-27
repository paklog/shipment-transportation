package com.paklog.shipment.application.exception;

public class ShipmentCreationException extends RuntimeException {
    public ShipmentCreationException(String message) {
        super(message);
    }

    public ShipmentCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}