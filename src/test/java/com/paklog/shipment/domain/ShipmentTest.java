package com.paklog.shipment.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShipmentTest {

    @Test
    void testShipmentCreation() {
        // Arrange
        ShipmentId shipmentId = ShipmentId.newId();
        OrderId orderId = OrderId.of("order-123");

        // Act
        Shipment shipment = new Shipment(shipmentId, orderId);

        // Assert
        assertNotNull(shipment);
        assertEquals(shipmentId, shipment.getId());
        assertEquals(orderId, shipment.getOrderId());
        assertEquals(ShipmentStatus.CREATED, shipment.getStatus());
        assertTrue(shipment.getTrackingEvents().isEmpty());
    }

    @Test
    void testShipmentCreationWithTracking() {
        // Arrange
        ShipmentId shipmentId = ShipmentId.newId();
        OrderId orderId = OrderId.of("order-123");
        TrackingNumber trackingNumber = TrackingNumber.of("track-123");
        CarrierName carrierName = CarrierName.FEDEX;

        // Act
        Shipment shipment = new Shipment(shipmentId, orderId, trackingNumber, carrierName, ShipmentStatus.IN_TRANSIT);

        // Assert
        assertNotNull(shipment);
        assertEquals(shipmentId, shipment.getId());
        assertEquals(orderId, shipment.getOrderId());
        assertEquals(trackingNumber, shipment.getTrackingNumber());
        assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
        assertTrue(shipment.getTrackingEvents().isEmpty());
    }

    @Test
    void testAssignTrackingNumber() {
        // Arrange
        Shipment shipment = new Shipment(ShipmentId.newId(), OrderId.of("order-123"));
        TrackingNumber trackingNumber = TrackingNumber.of("track-123");

        // Act
        shipment.assignTrackingNumber(trackingNumber);

        // Assert
        assertEquals(trackingNumber, shipment.getTrackingNumber());
        assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
    }

    @Test
    void testAddTrackingEvent() {
        // Arrange
        Shipment shipment = new Shipment(ShipmentId.newId(), OrderId.of("order-123"));
        TrackingEvent event = new TrackingEvent("status", "desc", "loc", java.time.Instant.now(), "CODE", "Detailed Description");

        // Act
        shipment.addTrackingEvent(event);

        // Assert
        assertEquals(1, shipment.getTrackingEvents().size());
        assertEquals(event, shipment.getTrackingEvents().get(0));
    }

    @Test
    void testMarkAsDelivered() {
        // Arrange
        Shipment shipment = new Shipment(ShipmentId.newId(), OrderId.of("order-123"));

        // Act
        shipment.markAsDelivered();

        // Assert
        assertEquals(ShipmentStatus.DELIVERED, shipment.getStatus());
    }

    @Test
    void testMarkDeliveryFailed() {
        // Arrange
        Shipment shipment = new Shipment(ShipmentId.newId(), OrderId.of("order-123"));

        // Act
        shipment.markDeliveryFailed();

        // Assert
        assertEquals(ShipmentStatus.FAILED_DELIVERY, shipment.getStatus());
    }

    @Test
    void testSetStatus() {
        // Arrange
        Shipment shipment = new Shipment(ShipmentId.newId(), OrderId.of("order-123"));

        // Act
        shipment.setStatus(ShipmentStatus.DELIVERED);

        // Assert
        assertEquals(ShipmentStatus.DELIVERED, shipment.getStatus());
    }

    @Test
    void testEquals() {
        // Arrange
        ShipmentId shipmentId = ShipmentId.newId();
        Shipment shipment1 = new Shipment(shipmentId, OrderId.of("order-123"));
        Shipment shipment2 = new Shipment(shipmentId, OrderId.of("order-456"));
        Shipment shipment3 = new Shipment(ShipmentId.newId(), OrderId.of("order-123"));

        // Assert
        assertEquals(shipment1, shipment2);
        assertNotEquals(shipment1, shipment3);
    }

    @Test
    void testHashCode() {
        // Arrange
        ShipmentId shipmentId = ShipmentId.newId();
        Shipment shipment1 = new Shipment(shipmentId, OrderId.of("order-123"));
        Shipment shipment2 = new Shipment(shipmentId, OrderId.of("order-456"));

        // Assert
        assertEquals(shipment1.hashCode(), shipment2.hashCode());
    }
}
