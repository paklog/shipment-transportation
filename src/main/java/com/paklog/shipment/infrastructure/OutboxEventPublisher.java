package com.paklog.shipment.infrastructure;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxEventPublisher {
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

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void processPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxService.getPendingEvents();
        for (OutboxEvent event : pendingEvents) {
            try {
                // Convert to CloudEvent format
                String cloudEvent = cloudEventFactory.createCloudEvent(
                    event.getAggregateType(),
                    event.getEventType(),
                    event.getPayload()
                );
                
                // Publish to Kafka
                kafkaEventProducer.publishEvent(event.getEventType(), cloudEvent);
                
                // Mark as processed
                outboxService.markEventAsProcessed(event.getId());
            } catch (Exception e) {
                outboxService.markEventAsFailed(event.getId(), e.getMessage());
            }
        }
    }
}