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

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String status;
        private String statusDescription;
        private String location;
        private OffsetDateTime timestamp;
        private String eventCode;
        private String detailedDescription;

        public Builder status(final String status) { this.status = status; return this; }
        public Builder statusDescription(final String statusDescription) { this.statusDescription = statusDescription; return this; }
        public Builder location(final String location) { this.location = location; return this; }
        public Builder timestamp(final OffsetDateTime timestamp) { this.timestamp = timestamp; return this; }
        public Builder eventCode(final String eventCode) { this.eventCode = eventCode; return this; }
        public Builder detailedDescription(final String detailedDescription) { this.detailedDescription = detailedDescription; return this; }

        public TrackingEvent build() {
            return new TrackingEvent(status, statusDescription, location, timestamp, eventCode, detailedDescription);
        }
    }
}
