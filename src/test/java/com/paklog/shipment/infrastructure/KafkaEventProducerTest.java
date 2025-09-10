package com.paklog.shipment.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.paklog.shipment.infrastructure.KafkaEventProducer.EventPublishingException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private KafkaEventProducer kafkaEventProducer;

    private String topic;
    private String event;

    @BeforeEach
    void setUp() {
        topic = "test-topic";
        event = "test-event-data";
    }

    @Test
    void testPublishEvent_Success() {
        // Arrange
        when(kafkaTemplate.send(topic, event)).thenReturn(mock(CompletableFuture.class)); // Mock a completed future

        // Act
        CompletableFuture<Void> resultFuture = kafkaEventProducer.publishEvent(topic, event);
        resultFuture.join();

        // Assert
        verify(kafkaTemplate, times(1)).send(topic, event);
        // No exception should be thrown
    }

    @Test
    void testPublishEvent_Failure() {
        // Arrange
        when(kafkaTemplate.send(topic, event)).thenThrow(new RuntimeException("Kafka error"));

        // Act & Assert
        CompletableFuture<Void> resultFuture = kafkaEventProducer.publishEvent(topic, event);
        java.util.concurrent.CompletionException completionException = assertThrows(java.util.concurrent.CompletionException.class, resultFuture::join);
        assertTrue(completionException.getCause() instanceof EventPublishingException);
        EventPublishingException thrown = (EventPublishingException) completionException.getCause();
        verify(kafkaTemplate, times(1)).send(topic, event);
        assertTrue(thrown.getMessage().contains("Failed to publish event"));
    }
}
