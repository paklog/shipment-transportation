package com.paklog.shipment.domain;

import java.time.Instant;

public class TrackingEvent {
    private final String status;
    private final String statusDescription;
    private final String location;
    private final Instant timestamp;
    private final String eventCode;
    private final String detailedDescription;

    public TrackingEvent(String status, String statusDescription, String location, Instant timestamp, String eventCode, String detailedDescription) {
        this.status = status;
        this.statusDescription = statusDescription;
        this.location = location;
        this.timestamp = timestamp;
        this.eventCode = eventCode;
        this.detailedDescription = detailedDescription;
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

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getEventCode() {
        return eventCode;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }
}