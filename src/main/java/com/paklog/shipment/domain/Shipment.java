package com.paklog.shipment.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Shipment {

    private final ShipmentId id;
    private final OrderId orderId;
    private final CarrierName carrierName;
    private final Instant createdAt;
    private final List<TrackingEvent> trackingEvents;

    private ShipmentStatus status;
    private TrackingNumber trackingNumber;
    private Instant dispatchedAt;
    private Instant deliveredAt;

    private Shipment(ShipmentId id, OrderId orderId, CarrierName carrierName, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Shipment id cannot be null");
        this.orderId = Objects.requireNonNull(orderId, "Order id cannot be null");
        this.carrierName = Objects.requireNonNull(carrierName, "Carrier cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation timestamp cannot be null");
        this.status = ShipmentStatus.CREATED;
        this.trackingEvents = new ArrayList<>();
    }

    public static Shipment create(OrderId orderId, CarrierName carrierName, Instant createdAt) {
        return new Shipment(ShipmentId.generate(), orderId, carrierName, createdAt);
    }

    public static Shipment create(OrderId orderId, CarrierName carrierName) {
        return create(orderId, carrierName, Instant.now());
    }

    public void dispatch(TrackingNumber trackingNumber, Instant dispatchedAt) {
        ensureStatus(ShipmentStatus.CREATED, "Shipment can only be dispatched from CREATED state");
        Objects.requireNonNull(trackingNumber, "Tracking number cannot be null when dispatching");
        if (this.trackingNumber != null) {
            throw new IllegalStateException("Shipment already has a tracking number");
        }
        this.trackingNumber = trackingNumber;
        this.dispatchedAt = Objects.requireNonNull(dispatchedAt, "dispatchedAt cannot be null");
        this.status = ShipmentStatus.DISPATCHED;
    }

    public void addTrackingEvent(TrackingEvent event) {
        Objects.requireNonNull(event, "Tracking event cannot be null");
        ensureHasTrackingNumber();
        ensureNotDelivered();
        if (!trackingEvents.isEmpty()) {
            Instant lastTimestamp = trackingEvents.get(trackingEvents.size() - 1).getTimestamp();
            if (!event.getTimestamp().isAfter(lastTimestamp)) {
                throw new IllegalArgumentException("Tracking events must be in chronological order");
            }
        }
        trackingEvents.add(event);
        if (status == ShipmentStatus.DISPATCHED || status == ShipmentStatus.CREATED) {
            status = ShipmentStatus.IN_TRANSIT;
        }
    }

    public void markAsDelivered(TrackingEvent deliveryEvent, Instant deliveredAt) {
        Objects.requireNonNull(deliveredAt, "Delivered timestamp cannot be null");
        ensureHasTrackingNumber();
        ensureNotDelivered();
        if (deliveryEvent != null) {
            addTrackingEvent(deliveryEvent);
        }
        this.status = ShipmentStatus.DELIVERED;
        this.deliveredAt = deliveredAt;
    }

    public void markDeliveryFailed(TrackingEvent failureEvent) {
        ensureHasTrackingNumber();
        if (failureEvent != null) {
            addTrackingEvent(failureEvent);
        }
        this.status = ShipmentStatus.FAILED_DELIVERY;
        this.deliveredAt = null;
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

    public boolean isDelivered() {
        return status == ShipmentStatus.DELIVERED;
    }

    private void ensureStatus(ShipmentStatus expected, String message) {
        if (status != expected) {
            throw new IllegalStateException(message);
        }
    }

    private void ensureNotDelivered() {
        if (status == ShipmentStatus.DELIVERED) {
            throw new IllegalStateException("Shipment is already delivered");
        }
        if (status == ShipmentStatus.FAILED_DELIVERY) {
            throw new IllegalStateException("Shipment is marked as delivery failed");
        }
    }

    private void ensureHasTrackingNumber() {
        if (trackingNumber == null) {
            throw new IllegalStateException("Tracking number must be assigned before recording tracking events");
        }
    }

    public static Shipment restore(ShipmentId id, OrderId orderId, CarrierName carrierName,
                                   TrackingNumber trackingNumber, ShipmentStatus status,
                                   Instant createdAt, Instant dispatchedAt, Instant deliveredAt,
                                   List<TrackingEvent> trackingEvents) {
        Shipment shipment = new Shipment(id, orderId, carrierName, createdAt);
        shipment.status = Objects.requireNonNull(status, "Shipment status cannot be null");
        shipment.trackingNumber = trackingNumber;
        shipment.dispatchedAt = dispatchedAt;
        shipment.deliveredAt = deliveredAt;
        shipment.trackingEvents.clear();
        if (trackingEvents != null) {
            shipment.trackingEvents.addAll(trackingEvents);
        }
        return shipment;
    }
}
