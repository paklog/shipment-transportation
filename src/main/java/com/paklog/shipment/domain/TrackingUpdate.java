package com.paklog.shipment.domain;

import java.util.List;
import java.util.Objects;

public record TrackingUpdate(
    TrackingEvent latestEvent,
    boolean isDelivered,
    List<TrackingEvent> newEvents
) {
    public TrackingUpdate {
        Objects.requireNonNull(latestEvent, "Latest event cannot be null");
        Objects.requireNonNull(newEvents, "New events list cannot be null");
    }

    public TrackingEvent getLatestEvent() {
        return latestEvent;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public List<TrackingEvent> getNewEvents() {
        return newEvents;
    }
}