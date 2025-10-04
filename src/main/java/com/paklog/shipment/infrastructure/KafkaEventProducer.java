package com.paklog.shipment.infrastructure;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Scope;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObservationRegistry observationRegistry;

    public KafkaEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                              ObservationRegistry observationRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.observationRegistry = observationRegistry;
    }

    public void publishEvent(String topic, String eventPayload) {
        Observation observation = Observation.createNotStarted("event.kafka.publish", observationRegistry)
                .contextualName("kafkaPublish")
                .lowCardinalityKeyValue(KeyValue.of("topic", topic))
                .start();
        try (Scope scope = observation.openScope()) {
            kafkaTemplate.send(topic, eventPayload).get();
            observation.lowCardinalityKeyValue(KeyValue.of("result", "success"));
            logger.debug("Published event to topic {}", topic);
        } catch (Exception e) {
            observation.lowCardinalityKeyValue(KeyValue.of("result", "error"));
            observation.error(e);
            logger.error("Failed to publish event to topic {}", topic, e);
            throw new EventPublishingException("Failed to publish event to topic " + topic, e);
        } finally {
            observation.stop();
        }
    }

    public static class EventPublishingException extends RuntimeException {
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
