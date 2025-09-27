package com.paklog.shipment.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Shipment {

    private final ShipmentId id;
    private final OrderId orderId;
    private ShipmentStatus status;
    private CarrierName carrierName;
    private TrackingNumber trackingNumber;
    private final List<TrackingEvent> trackingEvents;
    private Instant createdAt;
    private Instant dispatchedAt;
    private Instant deliveredAt;

    public Shipment(ShipmentId id, OrderId orderId) {
        this.id = Objects.requireNonNull(id);
        this.orderId = Objects.requireNonNull(orderId);
        this.status = ShipmentStatus.CREATED;
        this.trackingEvents = new ArrayList<>();
        this.createdAt = Instant.now();
    }

    private Shipment(ShipmentId id, OrderId orderId, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.orderId = Objects.requireNonNull(orderId);
        this.status = ShipmentStatus.CREATED;
        this.trackingEvents = new ArrayList<>();
        this.createdAt = createdAt;
    }

    public void assignTrackingNumber(TrackingNumber trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public void addTrackingEvent(TrackingEvent event) {
        this.trackingEvents.add(event);
    }

    public void dispatch(Instant dispatchedAt) {
        this.dispatchedAt = Objects.requireNonNull(dispatchedAt, "dispatchedAt cannot be null");
        this.status = ShipmentStatus.DISPATCHED;
    }

    public void deliver(Instant deliveredAt) {
        this.deliveredAt = Objects.requireNonNull(deliveredAt, "deliveredAt cannot be null");
        this.status = ShipmentStatus.DELIVERED;
    }

    public ShipmentId getId() {
        return id;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public CarrierName getCarrierName() {
        return carrierName;
    }

    public TrackingNumber getTrackingNumber() {
        return trackingNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public List<TrackingEvent> getTrackingEvents() {
        return Collections.unmodifiableList(trackingEvents);
    }

    public void setCarrierName(CarrierName carrierName) {
        this.carrierName = carrierName;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public static Shipment restore(ShipmentId id, OrderId orderId, CarrierName carrierName,
                                   TrackingNumber trackingNumber, ShipmentStatus status,
                                   Instant createdAt, Instant dispatchedAt, Instant deliveredAt,
                                   List<TrackingEvent> trackingEvents) {
        Shipment shipment = new Shipment(id, orderId, createdAt);
        shipment.carrierName = carrierName;
        shipment.trackingNumber = trackingNumber;
        shipment.status = status;
        shipment.dispatchedAt = dispatchedAt;
        shipment.deliveredAt = deliveredAt;
        shipment.trackingEvents.clear();
        if (trackingEvents != null) {
            shipment.trackingEvents.addAll(trackingEvents);
        }
        return shipment;
    }
}
