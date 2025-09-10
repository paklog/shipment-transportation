package com.paklog.shipment.adapter.fedex;

public class FedExTrackingRequest {
    private final String trackingNumber;

    public FedExTrackingRequest(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }
}
