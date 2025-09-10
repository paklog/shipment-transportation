package com.paklog.shipment.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class Shipment {
    private final ShipmentId id;
    private final OrderId orderId;
    @JsonUnwrapped
    private TrackingNumber trackingNumber;
    private ShipmentStatus status;
    private final List<TrackingEvent> trackingEvents;

    public Shipment(ShipmentId id, OrderId orderId, TrackingNumber trackingNumber, CarrierName carrierName, ShipmentStatus status) {
        if (id == null) throw new IllegalArgumentException("ShipmentId cannot be null");
        if (orderId == null) throw new IllegalArgumentException("OrderId cannot be null");
        this.id = id;
        this.orderId = orderId;
        this.trackingNumber = trackingNumber;
        this.status = status;
        this.trackingEvents = new ArrayList<>();
    }

    public Shipment(ShipmentId id, OrderId orderId) {
        if (id == null) throw new IllegalArgumentException("ShipmentId cannot be null");
        if (orderId == null) throw new IllegalArgumentException("OrderId cannot be null");
        this.id = id;
        this.orderId = orderId;
        this.status = ShipmentStatus.CREATED;
        this.trackingEvents = new ArrayList<>();
    }

    public void assignTrackingNumber(TrackingNumber trackingNumber) {
        if (trackingNumber == null) throw new IllegalArgumentException("TrackingNumber cannot be null");
        this.trackingNumber = trackingNumber;
        this.status = ShipmentStatus.IN_TRANSIT;
    }

    public void addTrackingEvent(TrackingEvent event) {
        if (event == null) throw new IllegalArgumentException("TrackingEvent cannot be null");
        trackingEvents.add(event);
    }

    public void markAsDelivered() {
        this.status = ShipmentStatus.DELIVERED;
    }

    public void markDeliveryFailed() {
        this.status = ShipmentStatus.FAILED_DELIVERY;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    // Getters
    public ShipmentId getId() { return id; }
    public OrderId getOrderId() { return orderId; }
    public TrackingNumber getTrackingNumber() { return trackingNumber; }
    public ShipmentStatus getStatus() { return status; }
    public List<TrackingEvent> getTrackingEvents() { 
        return Collections.unmodifiableList(trackingEvents); 
    }

    // equals and hashCode based on id
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shipment shipment = (Shipment) o;
        return Objects.equals(id, shipment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}