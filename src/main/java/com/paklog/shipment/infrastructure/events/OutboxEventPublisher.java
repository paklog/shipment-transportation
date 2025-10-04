package com.paklog.shipment.infrastructure.events;

import com.paklog.shipment.infrastructure.KafkaEventProducer;
import com.paklog.shipment.infrastructure.OutboxEvent;
import com.paklog.shipment.infrastructure.OutboxService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxService outboxService;
    private final CloudEventFactory cloudEventFactory;
    private final KafkaEventProducer kafkaEventProducer;

    public OutboxEventPublisher(OutboxService outboxService,
                                CloudEventFactory cloudEventFactory,
                                KafkaEventProducer kafkaEventProducer) {
        this.outboxService = outboxService;
        this.cloudEventFactory = cloudEventFactory;
        this.kafkaEventProducer = kafkaEventProducer;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.interval:30000}")
    public void processPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxService.getPendingEvents();
        for (OutboxEvent event : pendingEvents) {
            try {
                String payload = cloudEventFactory.toCloudEventJson(event);
                kafkaEventProducer.publishEvent(event.getDestination(), payload);
                outboxService.markEventAsProcessed(event.getId());
                logger.debug("Published {} for aggregate {} to {}", event.getEventType(), event.getAggregateId(), event.getDestination());
            } catch (Exception ex) {
                outboxService.markEventAsFailed(event.getId(), ex.getMessage());
                logger.warn("Failed to publish outbox event {}: {}", event.getId(), ex.getMessage());
            }
        }
    }
}
