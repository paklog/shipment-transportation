package com.paklog.shipment.infrastructure;

import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.domain.events.PackagePackedCloudEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackagePackedEventConsumerTest {

    @Mock
    private ShipmentApplicationService shipmentApplicationService;

    @InjectMocks
    private PackagePackedEventConsumer packagePackedEventConsumer;

    private PackagePackedCloudEvent mockEvent;

    @BeforeEach
    void setUp() {
        mockEvent = new PackagePackedCloudEvent("pkg-123", "ord-456", java.time.Instant.now());
    }

    @Test
    void testPackagePackedConsumer() {
        // Arrange
        Consumer<PackagePackedCloudEvent> consumer = packagePackedEventConsumer.packagePacked();

        // Act
        consumer.accept(mockEvent);

        // Assert
        verify(shipmentApplicationService, times(1)).createShipmentFromPackage(mockEvent);
    }
}
