package com.paklog.shipment.domain.events;

import java.time.Instant;

public class PackagePackedCloudEvent {
    private String packageId;
    private String orderId;
    private Instant packedAt;

    public PackagePackedCloudEvent() {}

    public PackagePackedCloudEvent(String packageId, String orderId, Instant packedAt) {
        this.packageId = packageId;
        this.orderId = orderId;
        this.packedAt = packedAt;
    }

    public String getPackageId() { return packageId; }
    public void setPackageId(String packageId) { this.packageId = packageId; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Instant getPackedAt() { return packedAt; }
    public void setPackedAt(Instant packedAt) { this.packedAt = packedAt; }
}