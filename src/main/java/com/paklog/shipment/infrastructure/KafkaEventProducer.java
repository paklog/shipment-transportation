package com.paklog.shipment.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEvent(String topic, String eventPayload) {
        try {
            kafkaTemplate.send(topic, eventPayload).get();
            logger.debug("Published event to topic {}", topic);
        } catch (Exception e) {
            logger.error("Failed to publish event to topic {}", topic, e);
            throw new EventPublishingException("Failed to publish event to topic " + topic, e);
        }
    }

    public static class EventPublishingException extends RuntimeException {
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
