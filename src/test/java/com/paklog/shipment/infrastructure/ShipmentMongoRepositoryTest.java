package com.paklog.shipment.infrastructure;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.infrastructure.persistence.ShipmentDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentMongoRepositoryTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private ShipmentMongoRepository shipmentMongoRepository;

    private ShipmentId shipmentId;
    private OrderId orderId;
    private TrackingNumber trackingNumber;
    private Shipment mockShipment;
    private ShipmentDocument mockShipmentDocument;

    @BeforeEach
    void setUp() {
        shipmentId = ShipmentId.generate();
        orderId = OrderId.of("order-123");
        trackingNumber = TrackingNumber.of("track-456");
        Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
        Instant dispatchedAt = Instant.parse("2024-01-01T01:00:00Z");
        mockShipment = Shipment.restore(
                shipmentId,
                orderId,
                CarrierName.FEDEX,
                trackingNumber,
                "label".getBytes(),
                ShipmentStatus.DISPATCHED,
                createdAt,
                dispatchedAt,
                null,
                java.util.List.of()
        );
        mockShipmentDocument = ShipmentDocument.fromDomain(mockShipment);
    }

    @Test
    void testSave() {
        // Arrange
        when(mongoTemplate.save(any(ShipmentDocument.class))).thenReturn(mockShipmentDocument);

        // Act
        Shipment savedShipment = shipmentMongoRepository.save(mockShipment);

        // Assert
        assertNotNull(savedShipment);
        assertEquals(mockShipment.getTrackingNumber(), savedShipment.getTrackingNumber());
        assertEquals(mockShipment.getCarrierName(), savedShipment.getCarrierName());
        assertArrayEquals(mockShipment.getLabelData(), savedShipment.getLabelData());
        verify(mongoTemplate, times(1)).save(any(ShipmentDocument.class));
    }

    @Test
    void testFindById_Found() {
        // Arrange
        when(mongoTemplate.findById(shipmentId.getValue(), ShipmentDocument.class)).thenReturn(mockShipmentDocument);

        // Act
        Optional<Shipment> foundShipment = shipmentMongoRepository.findById(shipmentId);

        // Assert
        assertTrue(foundShipment.isPresent());
        assertEquals(mockShipment.getTrackingNumber(), foundShipment.get().getTrackingNumber());
        verify(mongoTemplate, times(1)).findById(shipmentId.getValue(), ShipmentDocument.class);
    }

    @Test
    void testFindById_NotFound() {
        // Arrange
        when(mongoTemplate.findById(shipmentId.getValue(), ShipmentDocument.class)).thenReturn(null);

        // Act
        Optional<Shipment> foundShipment = shipmentMongoRepository.findById(shipmentId);

        // Assert
        assertFalse(foundShipment.isPresent());
        verify(mongoTemplate, times(1)).findById(shipmentId.getValue(), ShipmentDocument.class);
    }

    @Test
    void testFindByTrackingNumber_Found() {
        // Arrange
        when(mongoTemplate.findOne(any(Query.class), eq(ShipmentDocument.class))).thenReturn(mockShipmentDocument);

        // Act
        Optional<Shipment> foundShipment = shipmentMongoRepository.findByTrackingNumber(trackingNumber);

        // Assert
        assertTrue(foundShipment.isPresent());
        assertEquals(mockShipment.getTrackingNumber(), foundShipment.get().getTrackingNumber());
        verify(mongoTemplate, times(1)).findOne(any(Query.class), eq(ShipmentDocument.class));
    }

    @Test
    void testFindByTrackingNumber_NotFound() {
        // Arrange
        when(mongoTemplate.findOne(any(Query.class), eq(ShipmentDocument.class))).thenReturn(null);

        // Act
        Optional<Shipment> foundShipment = shipmentMongoRepository.findByTrackingNumber(trackingNumber);

        // Assert
        assertFalse(foundShipment.isPresent());
        verify(mongoTemplate, times(1)).findOne(any(Query.class), eq(ShipmentDocument.class));
    }
}
