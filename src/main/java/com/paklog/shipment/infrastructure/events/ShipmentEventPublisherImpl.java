package com.paklog.shipment.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.paklog.shipment.application.port.ShipmentEventPublisher;
import com.paklog.shipment.config.ShipmentEventProperties;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.infrastructure.OutboxService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ShipmentEventPublisherImpl implements ShipmentEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentEventPublisherImpl.class);

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final ShipmentEventProperties shipmentEventProperties;

    public ShipmentEventPublisherImpl(OutboxService outboxService,
                                      ObjectMapper objectMapper,
                                      ShipmentEventProperties shipmentEventProperties) {
        this.outboxService = outboxService;
        this.objectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.shipmentEventProperties = shipmentEventProperties;
    }

    @Override
    public void shipmentDispatched(Shipment shipment) {
        ShipmentDispatchedPayload payload = new ShipmentDispatchedPayload(
                shipment.getId().toString(),
                shipment.getOrderId().getValue(),
                shipment.getCarrierName().name(),
                shipment.getTrackingNumber().getValue(),
                shipment.getDispatchedAt()
        );
        ShipmentEventProperties.EventProperties dispatched = shipmentEventProperties.getDispatched();
        persistEvent(shipment, dispatched.getType(), dispatched.getTopic(), payload);
    }

    @Override
    public void shipmentDelivered(Shipment shipment) {
        ShipmentDeliveredPayload payload = new ShipmentDeliveredPayload(
                shipment.getId().toString(),
                shipment.getOrderId().getValue(),
                shipment.getCarrierName().name(),
                shipment.getTrackingNumber().getValue(),
                shipment.getDeliveredAt()
        );
        ShipmentEventProperties.EventProperties delivered = shipmentEventProperties.getDelivered();
        persistEvent(shipment, delivered.getType(), delivered.getTopic(), payload);
    }

    private void persistEvent(Shipment shipment,
                              String eventType,
                              String destination,
                              Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            outboxService.save(new SimpleDomainEvent(
                    shipment.getId().toString(),
                    "Shipment",
                    eventType,
                    destination,
                    jsonPayload
            ));
            logger.debug("Queued {} event for shipment {}", eventType, shipment.getId());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise shipment event payload", e);
        }
    }

    private record ShipmentDispatchedPayload(String shipmentId,
                                             String orderId,
                                             String carrier,
                                             String trackingNumber,
                                             Instant dispatchedAt) {}

    private record ShipmentDeliveredPayload(String shipmentId,
                                            String orderId,
                                            String carrier,
                                            String trackingNumber,
                                            Instant deliveredAt) {}
}
