package com.paklog.shipment.domain.exception;

public class CarrierException extends RuntimeException {
    public CarrierException(String message, Throwable cause) {
        super(message, cause);
    }
}
