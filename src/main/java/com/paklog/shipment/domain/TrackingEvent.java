package com.paklog.shipment.domain;

import java.time.OffsetDateTime;
import java.util.Objects;

public class TrackingEvent {
    private final String status;
    private final String statusDescription;
    private final String location;
    private final OffsetDateTime timestamp;
    private final String eventCode;
    private final String detailedDescription;

    public TrackingEvent(String status, String statusDescription, String location, OffsetDateTime timestamp, String eventCode, String detailedDescription) {
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

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public String getEventCode() {
        return eventCode;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }
}
