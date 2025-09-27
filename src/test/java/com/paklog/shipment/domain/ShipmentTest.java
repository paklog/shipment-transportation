package com.paklog.shipment.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentTest {

    @Test
    void newShipmentInitialisesWithDefaults() {
        ShipmentId shipmentId = ShipmentId.newId();
        OrderId orderId = OrderId.of("order-123");

        Shipment shipment = Shipment.newShipment(shipmentId, orderId, CarrierName.FEDEX);

        assertEquals(shipmentId, shipment.getId());
        assertEquals(orderId, shipment.getOrderId());
        assertEquals(CarrierName.FEDEX, shipment.getCarrierName());
        assertEquals(ShipmentStatus.CREATED, shipment.getStatus());
        assertNotNull(shipment.getCreatedAt());
        assertNull(shipment.getTrackingNumber());
        assertTrue(shipment.getTrackingEvents().isEmpty());
    }

    @Test
    void assignTrackingNumberTransitionsToInTransit() {
        Shipment shipment = Shipment.newShipment(ShipmentId.newId(), OrderId.of("order-123"), CarrierName.UPS);
        TrackingNumber trackingNumber = TrackingNumber.of("TRACK123");

        shipment.assignTrackingNumber(trackingNumber);

        assertEquals(trackingNumber, shipment.getTrackingNumber());
        assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
        assertNotNull(shipment.getDispatchedAt());
    }

    @Test
    void addTrackingEventStoresEvent() {
        Shipment shipment = Shipment.newShipment(ShipmentId.newId(), OrderId.of("order-123"), CarrierName.UPS);
        TrackingEvent event = new TrackingEvent("IN_TRANSIT", "Departed facility", "New York", Instant.now(), "CODE", "Detailed");

        shipment.addTrackingEvent(event);

        assertEquals(1, shipment.getTrackingEvents().size());
        assertEquals(event, shipment.getTrackingEvents().get(0));
    }

    @Test
    void markAsDeliveredSetsDeliveredTimestamp() {
        Shipment shipment = Shipment.newShipment(ShipmentId.newId(), OrderId.of("order-123"), CarrierName.UPS);
        shipment.assignTrackingNumber(TrackingNumber.of("TRACK123"));
        Instant deliveredAt = Instant.now();

        shipment.markAsDelivered(deliveredAt);

        assertEquals(ShipmentStatus.DELIVERED, shipment.getStatus());
        assertEquals(deliveredAt, shipment.getDeliveredAt());
    }

    @Test
    void markDeliveryFailedClearsDeliveredTimestamp() {
        Shipment shipment = Shipment.newShipment(ShipmentId.newId(), OrderId.of("order-123"), CarrierName.UPS);
        shipment.assignTrackingNumber(TrackingNumber.of("TRACK123"));
        shipment.markAsDelivered(Instant.now());

        shipment.markDeliveryFailed();

        assertEquals(ShipmentStatus.FAILED_DELIVERY, shipment.getStatus());
        assertNull(shipment.getDeliveredAt());
    }

    @Test
    void restoreRehydratesAggregate() {
        ShipmentId shipmentId = ShipmentId.newId();
        OrderId orderId = OrderId.of("order-123");
        TrackingNumber trackingNumber = TrackingNumber.of("TRACK123");
        Instant createdAt = Instant.now().minusSeconds(3600);
        Instant dispatchedAt = createdAt.plusSeconds(600);
        Instant deliveredAt = dispatchedAt.plusSeconds(600);
        TrackingEvent event = new TrackingEvent("DELIVERED", "Package delivered", "Los Angeles", deliveredAt, "DEL", "Left at door");

        Shipment shipment = Shipment.restore(
            shipmentId,
            orderId,
            CarrierName.FEDEX,
            trackingNumber,
            ShipmentStatus.DELIVERED,
            createdAt,
            dispatchedAt,
            deliveredAt,
            List.of(event)
        );

        assertEquals(shipmentId, shipment.getId());
        assertEquals(orderId, shipment.getOrderId());
        assertEquals(CarrierName.FEDEX, shipment.getCarrierName());
        assertEquals(trackingNumber, shipment.getTrackingNumber());
        assertEquals(ShipmentStatus.DELIVERED, shipment.getStatus());
        assertEquals(createdAt, shipment.getCreatedAt());
        assertEquals(dispatchedAt, shipment.getDispatchedAt());
        assertEquals(deliveredAt, shipment.getDeliveredAt());
        assertEquals(1, shipment.getTrackingEvents().size());
    }

    @Test
    void equalsAndHashCodeUseIdentity() {
        ShipmentId shipmentId = ShipmentId.newId();
        Shipment shipment1 = Shipment.newShipment(shipmentId, OrderId.of("order-123"), CarrierName.FEDEX);
        Shipment shipment2 = Shipment.newShipment(shipmentId, OrderId.of("order-456"), CarrierName.FEDEX);

        assertEquals(shipment1, shipment2);
        assertEquals(shipment1.hashCode(), shipment2.hashCode());
    }
}
