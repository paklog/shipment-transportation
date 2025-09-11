package com.paklog.shipment.domain.exception;

public class CarrierException extends RuntimeException {
    private final String carrierName;
    private final String errorCode;

    public CarrierException(String message, String carrierName) {
        super(message);
        this.carrierName = carrierName;
        this.errorCode = null;
    }

    public CarrierException(String message, String carrierName, String errorCode, Throwable cause) {
        super(message, cause);
        this.carrierName = carrierName;
        this.errorCode = errorCode;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public String getErrorCode() {
        return errorCode;
    }
}