package com.paklog.shipment.infrastructure.events;

import com.paklog.shipment.infrastructure.KafkaEventProducer;
import com.paklog.shipment.infrastructure.OutboxEvent;
import com.paklog.shipment.infrastructure.OutboxService;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.Observation.Scope;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxService outboxService;
    private final CloudEventSerializer cloudEventSerializer;
    private final KafkaEventProducer kafkaEventProducer;
    private final ObservationRegistry observationRegistry;
    private final Tracer tracer;

    public OutboxEventPublisher(OutboxService outboxService,
                                CloudEventSerializer cloudEventSerializer,
                                KafkaEventProducer kafkaEventProducer,
                                ObservationRegistry observationRegistry,
                                Tracer tracer) {
        this.outboxService = outboxService;
        this.cloudEventSerializer = cloudEventSerializer;
        this.kafkaEventProducer = kafkaEventProducer;
        this.observationRegistry = observationRegistry;
        this.tracer = tracer;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.interval:30000}")
    public void processPendingEvents() {
        Observation batchObservation = Observation.createNotStarted("outbox.publish.batch", observationRegistry)
                .contextualName("outboxPublisher")
                .start();

        int failures = 0;
        List<OutboxEvent> pendingEvents = outboxService.getPendingEvents();
        batchObservation.highCardinalityKeyValue(KeyValue.of("events.count", Integer.toString(pendingEvents.size())));

        try (Scope scope = batchObservation.openScope()) {
            if (pendingEvents.isEmpty()) {
                batchObservation.lowCardinalityKeyValue(KeyValue.of("result", "noop"));
                return;
            }

            for (OutboxEvent event : pendingEvents) {
                Observation eventObservation = Observation.createNotStarted("outbox.publish.event", observationRegistry)
                        .contextualName("publishOutboxEvent")
                        .lowCardinalityKeyValue(KeyValue.of("destination", event.getDestination()))
                        .highCardinalityKeyValue(KeyValue.of("outbox.id", event.getId()))
                        .start();

                try (Scope eventScope = eventObservation.openScope()) {
                    String payload = cloudEventSerializer.serialize(
                            event.getId(),
                            event.getAggregateId(),
                            event.getAggregateType(),
                            event.getEventType(),
                            event.getPayload(),
                            currentTraceContext());
                    kafkaEventProducer.publishEvent(event.getDestination(), payload);
                    outboxService.markEventAsProcessed(event.getId());
                    eventObservation.lowCardinalityKeyValue(KeyValue.of("result", "success"));
                    logger.debug("Published {} for aggregate {} to {}", event.getEventType(), event.getAggregateId(), event.getDestination());
                } catch (Exception ex) {
                    failures++;
                    eventObservation.lowCardinalityKeyValue(KeyValue.of("result", "error"));
                    eventObservation.error(ex);
                    outboxService.markEventAsFailed(event.getId(), ex.getMessage());
                    logger.warn("Failed to publish outbox event {}: {}", event.getId(), ex.getMessage());
                } finally {
                    eventObservation.stop();
                }
            }

            batchObservation.lowCardinalityKeyValue(KeyValue.of("result", failures == 0 ? "success" : "partial"));
            batchObservation.highCardinalityKeyValue(KeyValue.of("events.failed", Integer.toString(failures)));
        } finally {
            batchObservation.stop();
        }
    }

    private TraceContext currentTraceContext() {
        return tracer.currentSpan() != null ? tracer.currentSpan().context() : null;
    }
}
