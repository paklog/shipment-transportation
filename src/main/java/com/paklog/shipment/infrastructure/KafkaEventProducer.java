package com.paklog.shipment.infrastructure;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

@Component
public class KafkaEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public CompletableFuture<Void> publishEvent(String topic, String event) {
        return CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(topic, event).get();
                System.out.println("Successfully published event to topic: " + topic);
            } catch (Exception e) {
                System.err.println("Failed to publish event to topic " + topic + ": " + e.getMessage());
                throw new EventPublishingException("Failed to publish event", e);
            }
        });
    }

    public static class EventPublishingException extends RuntimeException {
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}