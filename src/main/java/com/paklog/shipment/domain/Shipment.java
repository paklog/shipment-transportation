package com.paklog.shipment.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Shipment {

    private final ShipmentId id;
    private final OrderId orderId;
    private ShipmentStatus status;
    private CarrierName carrierName; // Added
    private TrackingNumber trackingNumber; // Added
    private final List<TrackingEvent> trackingEvents;

    public Shipment(ShipmentId id, OrderId orderId) {
        this.id = Objects.requireNonNull(id);
        this.orderId = Objects.requireNonNull(orderId);
        this.status = ShipmentStatus.CREATED;
        this.trackingEvents = new ArrayList<>();
    }

    public void assignTrackingNumber(TrackingNumber trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public void addTrackingEvent(TrackingEvent event) {
        this.trackingEvents.add(event);
    }

    // Getters
    public ShipmentId getId() {
        return id;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public List<TrackingEvent> getTrackingEvents() {
        return Collections.unmodifiableList(trackingEvents);
    }

    public CarrierName getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(CarrierName carrierName) {
        this.carrierName = carrierName;
    }

    public TrackingNumber getTrackingNumber() {
        return trackingNumber;
    }
}
