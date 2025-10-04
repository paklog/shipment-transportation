package com.paklog.shipment.domain;

import java.util.Objects;

public class CarrierInfo {
    private final String trackingNumber;
    private final byte[] labelData;
    private final CarrierName carrierName;

    public CarrierInfo(String trackingNumber, byte[] labelData, CarrierName carrierName) {
        this.trackingNumber = Objects.requireNonNull(trackingNumber, "Tracking number cannot be null");
        this.labelData = Objects.requireNonNull(labelData, "Label data cannot be null");
        this.carrierName = Objects.requireNonNull(carrierName, "Carrier name cannot be null");
        if (labelData.length == 0) {
            throw new IllegalArgumentException("Label data cannot be empty");
        }
        if (trackingNumber.isBlank()) {
            throw new IllegalArgumentException("Tracking number cannot be blank");
        }
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public byte[] getLabelData() {
        return labelData;
    }

    public CarrierName getCarrierName() {
        return carrierName;
    }
}
