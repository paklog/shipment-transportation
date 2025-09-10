package com.paklog.shipment.adapter.fedex;

public class FedExShipmentResponse {
    private final String trackingNumber;
    private final byte[] labelData;

    public FedExShipmentResponse(String trackingNumber, byte[] labelData) {
        this.trackingNumber = trackingNumber;
        this.labelData = labelData;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public byte[] getLabelData() {
        return labelData;
    }
}
