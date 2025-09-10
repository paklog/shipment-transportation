package com.paklog.shipment.application.command;

import jakarta.validation.constraints.NotBlank;

public class CreateShipmentCommand {
    @NotBlank(message = "Package ID cannot be blank")
    private String packageId;

    @NotBlank(message = "Order ID cannot be blank")
    private String orderId;

    public CreateShipmentCommand(String packageId, String orderId) {
        this.packageId = packageId;
        this.orderId = orderId;
    }

    public String getPackageId() {
        return packageId;
    }

    public String getOrderId() {
        return orderId;
    }
}
