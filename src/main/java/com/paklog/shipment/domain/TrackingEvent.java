package com.paklog.shipment.domain;

import java.time.Instant;
import java.util.Objects;

public class TrackingEvent {
    private final String status;
    private final String statusDescription;
    private final String location;
    private final Instant timestamp;
    private final String eventCode;
    private final String detailedDescription;

    public TrackingEvent(String status, String statusDescription, String location, Instant timestamp, String eventCode, String detailedDescription) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.statusDescription = Objects.requireNonNull(statusDescription, "statusDescription cannot be null");
        this.location = Objects.requireNonNull(location, "location cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
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
