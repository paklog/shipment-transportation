package com.paklog.shipment.adapter.fedex;

import java.time.Instant;

public class FedExTrackingResponse {
    private final String status;
    private final String statusDescription;
    private final String location;
    private final Instant lastUpdated;

    public FedExTrackingResponse(String status, String statusDescription, String location, Instant lastUpdated) {
        this.status = status;
        this.statusDescription = statusDescription;
        this.location = location;
        this.lastUpdated = lastUpdated;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public String getLocation() {
        return location;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }
}
