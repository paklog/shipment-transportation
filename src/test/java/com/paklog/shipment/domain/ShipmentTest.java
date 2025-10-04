package com.paklog.shipment.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentTest {

    private static final OrderId ORDER_ID = OrderId.of("order-123");
    private static final CarrierName CARRIER = CarrierName.FEDEX;

    @Test
    void createShipmentInitialisesWithDefaults() {
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");

        Shipment shipment = Shipment.create(ORDER_ID, CARRIER, createdAt);

        assertNotNull(shipment.getId());
        assertEquals(ORDER_ID, shipment.getOrderId());
        assertEquals(CARRIER, shipment.getCarrierName());
        assertEquals(ShipmentStatus.CREATED, shipment.getStatus());
        assertEquals(createdAt, shipment.getCreatedAt());
        assertNull(shipment.getTrackingNumber());
        assertTrue(shipment.getTrackingEvents().isEmpty());
    }

    @Test
    void dispatchAssignsTrackingNumberAndTimestamp() {
        Shipment shipment = Shipment.create(ORDER_ID, CARRIER, Instant.parse("2024-01-01T00:00:00Z"));
        TrackingNumber trackingNumber = TrackingNumber.of("TRACK123");
        Instant dispatchTime = Instant.parse("2024-01-01T01:00:00Z");

        shipment.dispatch(trackingNumber, dispatchTime);

        assertEquals(trackingNumber, shipment.getTrackingNumber());
        assertEquals(ShipmentStatus.DISPATCHED, shipment.getStatus());
        assertEquals(dispatchTime, shipment.getDispatchedAt());
    }

    @Test
    void addTrackingEventRequiresTrackingNumber() {
        Shipment shipment = Shipment.create(ORDER_ID, CARRIER, Instant.parse("2024-01-01T00:00:00Z"));
        TrackingEvent event = new TrackingEvent("IN_TRANSIT", "Departed facility", "New York",
                Instant.parse("2024-01-01T01:00:00Z"), "CODE", "Detailed");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> shipment.addTrackingEvent(event));
        assertEquals("Tracking number must be assigned before recording tracking events", thrown.getMessage());
    }

    @Test
    void addTrackingEventAppendsChronologicallyAndMovesToInTransit() {
        Shipment shipment = Shipment.create(ORDER_ID, CARRIER, Instant.parse("2024-01-01T00:00:00Z"));
        shipment.dispatch(TrackingNumber.of("TRACK123"), Instant.parse("2024-01-01T01:00:00Z"));

        TrackingEvent firstEvent = new TrackingEvent("IN_TRANSIT", "Departed facility", "New York",
                Instant.parse("2024-01-01T02:00:00Z"), "CODE", "Detailed");
        shipment.addTrackingEvent(firstEvent);

        TrackingEvent secondEvent = new TrackingEvent("IN_TRANSIT", "Arrived at hub", "Chicago",
                Instant.parse("2024-01-01T03:00:00Z"), "CODE2", "Details");
        shipment.addTrackingEvent(secondEvent);

        assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
        assertEquals(List.of(firstEvent, secondEvent), shipment.getTrackingEvents());
    }

    @Test
    void addTrackingEventRejectsNonChronologicalTimestamps() {
        Shipment shipment = Shipment.create(ORDER_ID, CARRIER, Instant.parse("2024-01-01T00:00:00Z"));
        shipment.dispatch(TrackingNumber.of("TRACK123"), Instant.parse("2024-01-01T01:00:00Z"));

        TrackingEvent firstEvent = new TrackingEvent("IN_TRANSIT", "Departed", "New York",
                Instant.parse("2024-01-01T02:00:00Z"), "CODE", "Details");
        shipment.addTrackingEvent(firstEvent);

        TrackingEvent earlierEvent = new TrackingEvent("IN_TRANSIT", "Arrived", "Boston",
                Instant.parse("2024-01-01T01:30:00Z"), "CODE2", "Earlier");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> shipment.addTrackingEvent(earlierEvent));
        assertEquals("Tracking events must be in chronological order", thrown.getMessage());
    }

    @Test
    void markAsDeliveredRecordsDeliveryEventAndTimestamp() {
        Shipment shipment = Shipment.create(ORDER_ID, CARRIER, Instant.parse("2024-01-01T00:00:00Z"));
        shipment.dispatch(TrackingNumber.of("TRACK123"), Instant.parse("2024-01-01T01:00:00Z"));

        TrackingEvent deliveryEvent = new TrackingEvent("DELIVERED", "Package delivered", "Los Angeles",
                Instant.parse("2024-01-02T10:00:00Z"), "DEL", "Left at door");

        shipment.markAsDelivered(deliveryEvent, deliveryEvent.getTimestamp());

        assertEquals(ShipmentStatus.DELIVERED, shipment.getStatus());
        assertEquals(deliveryEvent.getTimestamp(), shipment.getDeliveredAt());
        assertTrue(shipment.getTrackingEvents().contains(deliveryEvent));
    }

    @Test
    void markDeliveryFailedTransitionsState() {
        Shipment shipment = Shipment.create(ORDER_ID, CARRIER, Instant.parse("2024-01-01T00:00:00Z"));
        shipment.dispatch(TrackingNumber.of("TRACK123"), Instant.parse("2024-01-01T01:00:00Z"));

        TrackingEvent failureEvent = new TrackingEvent("FAILED_DELIVERY", "Customer unavailable", "Los Angeles",
                Instant.parse("2024-01-02T10:00:00Z"), "FAIL", "Left notice");

        shipment.markDeliveryFailed(failureEvent);

        assertEquals(ShipmentStatus.FAILED_DELIVERY, shipment.getStatus());
        assertNull(shipment.getDeliveredAt());
        assertTrue(shipment.getTrackingEvents().contains(failureEvent));
    }

    @Test
    void restoreRehydratesAggregateState() {
        ShipmentId shipmentId = ShipmentId.generate();
        TrackingNumber trackingNumber = TrackingNumber.of("TRACK123");
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        Instant dispatchedAt = Instant.parse("2024-01-01T01:00:00Z");
        Instant deliveredAt = Instant.parse("2024-01-02T10:00:00Z");
        TrackingEvent event = new TrackingEvent("DELIVERED", "Package delivered", "Los Angeles",
                deliveredAt, "DEL", "Left at door");

        Shipment shipment = Shipment.restore(
                shipmentId,
                ORDER_ID,
                CARRIER,
                trackingNumber,
                ShipmentStatus.DELIVERED,
                createdAt,
                dispatchedAt,
                deliveredAt,
                List.of(event)
        );

        assertEquals(shipmentId, shipment.getId());
        assertEquals(ORDER_ID, shipment.getOrderId());
        assertEquals(CARRIER, shipment.getCarrierName());
        assertEquals(trackingNumber, shipment.getTrackingNumber());
        assertEquals(ShipmentStatus.DELIVERED, shipment.getStatus());
        assertEquals(createdAt, shipment.getCreatedAt());
        assertEquals(dispatchedAt, shipment.getDispatchedAt());
        assertEquals(deliveredAt, shipment.getDeliveredAt());
        assertEquals(List.of(event), shipment.getTrackingEvents());
    }
}
