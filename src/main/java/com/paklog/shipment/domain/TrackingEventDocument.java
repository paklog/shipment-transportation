package com.paklog.shipment.domain;

import java.time.Instant;

public class TrackingEventDocument {

    private String status;
    private String statusDescription;
    private String location;
    private Instant timestamp;
    private String eventCode;
    private String detailedDescription;

    public static TrackingEventDocument fromDomain(TrackingEvent event) {
        TrackingEventDocument doc = new TrackingEventDocument();
        doc.setStatus(event.getStatus());
        doc.setStatusDescription(event.getStatusDescription());
        doc.setLocation(event.getLocation());
        doc.setTimestamp(event.getTimestamp());
        doc.setEventCode(event.getEventCode());
        doc.setDetailedDescription(event.getDetailedDescription());
        return doc;
    }

    public TrackingEvent toDomain() {
        return new TrackingEvent(status, statusDescription, location, timestamp, eventCode, detailedDescription);
    }

    // Getters and setters

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public void setDetailedDescription(String detailedDescription) {
        this.detailedDescription = detailedDescription;
    }
}