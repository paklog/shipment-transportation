package com.paklog.shipment.adapter.fedex;

public class FedExShipmentRequest {
    private String packageId;
    private String orderId;
    private String serviceType = "FEDEX_GROUND"; // Default service
    
    public FedExShipmentRequest(String packageId, String orderId) {
        this.packageId = packageId;
        this.orderId = orderId;
    }

    // Getters
    public String getPackageId() { return packageId; }
    public String getOrderId() { return orderId; }
    public String getServiceType() { return serviceType; }
}