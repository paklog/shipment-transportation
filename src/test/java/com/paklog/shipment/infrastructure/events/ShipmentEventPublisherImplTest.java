package com.paklog.shipment.infrastructure.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.shipment.config.ShipmentEventProperties;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.domain.TrackingEvent;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.infrastructure.OutboxEvent;
import com.paklog.shipment.infrastructure.OutboxService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentEventPublisherImplTest {

    @Mock
    private OutboxService outboxService;

    private ShipmentEventPublisherImpl publisher;

    @BeforeEach
    void setUp() {
        ShipmentEventProperties properties = new ShipmentEventProperties();
        properties.getDispatched().setType("com.paklog.shipment.dispatched.v1");
        properties.getDispatched().setTopic("shipment-dispatched");
        properties.getDelivered().setType("com.paklog.shipment.delivered.v1");
        properties.getDelivered().setTopic("shipment-delivered");

        publisher = new ShipmentEventPublisherImpl(outboxService, new ObjectMapper(), properties);
        when(outboxService.save(any())).thenAnswer(invocation -> new OutboxEvent(invocation.getArgument(0)));
    }

    @Test
    void savesDispatchedEventToOutbox() {
        Shipment shipment = dispatchedShipment();

        publisher.shipmentDispatched(shipment);

        ArgumentCaptor<SimpleDomainEvent> captor = ArgumentCaptor.forClass(SimpleDomainEvent.class);
        verify(outboxService).save(captor.capture());
        SimpleDomainEvent event = captor.getValue();

        assertEquals(shipment.getId().toString(), event.getAggregateId());
        assertEquals("Shipment", event.getAggregateType());
        assertEquals("com.paklog.shipment.dispatched.v1", event.getEventType());
        assertEquals("shipment-dispatched", event.getDestination());
    }

    @Test
    void savesDeliveredEventToOutbox() {
        Shipment shipment = deliveredShipment();

        publisher.shipmentDelivered(shipment);

        ArgumentCaptor<SimpleDomainEvent> captor = ArgumentCaptor.forClass(SimpleDomainEvent.class);
        verify(outboxService).save(captor.capture());
        SimpleDomainEvent event = captor.getValue();

        assertEquals("com.paklog.shipment.delivered.v1", event.getEventType());
        assertEquals("shipment-delivered", event.getDestination());
    }

    private Shipment dispatchedShipment() {
        Shipment shipment = Shipment.create(OrderId.of("order-123"), CarrierName.FEDEX,
                Instant.parse("2024-01-01T00:00:00Z"));
        shipment.dispatch(TrackingNumber.of("trk-123"), Instant.parse("2024-01-01T01:00:00Z"));
        return shipment;
    }

    private Shipment deliveredShipment() {
        Shipment shipment = dispatchedShipment();
        TrackingEvent delivered = new TrackingEvent("DELIVERED", "Delivered", "LA",
                Instant.parse("2024-01-02T10:00:00Z"), "DEL", "Left at door");
        shipment.markAsDelivered(delivered, delivered.getTimestamp());
        return shipment;
    }
}
