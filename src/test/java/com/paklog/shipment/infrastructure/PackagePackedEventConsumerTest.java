package com.paklog.shipment.infrastructure;

import com.paklog.shipment.application.MetricsService;
import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.application.command.CreateShipmentCommand;
import com.paklog.shipment.domain.events.PackagePackedCloudEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PackagePackedEventConsumerTest {

    @Mock
    private ShipmentApplicationService shipmentApplicationService;

    private PackagePackedEventConsumer packagePackedEventConsumer;

    private MetricsService metricsService;

    private PackagePackedCloudEvent mockEvent;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService(new SimpleMeterRegistry());
        packagePackedEventConsumer = new PackagePackedEventConsumer(shipmentApplicationService, metricsService);
        mockEvent = new PackagePackedCloudEvent("pkg-123", "ord-456", java.time.Instant.now());
    }

    @Test
    void testPackagePackedConsumer() {
        // Arrange
        Consumer<PackagePackedCloudEvent> consumer = packagePackedEventConsumer.packagePacked();
        double before = metricsService.kafkaEventsConsumed.count();

        // Act
        consumer.accept(mockEvent);

        // Assert
        verify(shipmentApplicationService, times(1)).createShipment(argThat(command ->
                command.getPackageId().equals(mockEvent.getPackageId()) &&
                        command.getOrderIdAsString().equals(mockEvent.getOrderId())));
        double after = metricsService.kafkaEventsConsumed.count();
        assertEquals(before + 1, after);
    }
}
