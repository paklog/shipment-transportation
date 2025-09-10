package com.paklog.shipment.domain;

public class CarrierInfo {
    private final String trackingNumber;
    private final byte[] labelData;

    public CarrierInfo(String trackingNumber, byte[] labelData) {
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
