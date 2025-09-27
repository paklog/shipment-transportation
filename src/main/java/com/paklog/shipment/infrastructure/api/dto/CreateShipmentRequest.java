package com.paklog.shipment.infrastructure.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateShipmentRequest {

    @NotBlank(message = "packageId must not be blank")
    private String packageId;

    @NotBlank(message = "orderId must not be blank")
    private String orderId;

    public CreateShipmentRequest() {
    }

    public CreateShipmentRequest(String packageId, String orderId) {
        this.packageId = packageId;
        this.orderId = orderId;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
}
