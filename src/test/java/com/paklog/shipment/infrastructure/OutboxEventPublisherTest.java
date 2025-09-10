package com.paklog.shipment.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private CloudEventFactory cloudEventFactory;

    @Mock
    private KafkaEventProducer kafkaEventProducer;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    private OutboxEvent mockOutboxEvent;

    

    @Test
    void testProcessPendingEvents_Success() {
        // Arrange
        mockOutboxEvent = mock(OutboxEvent.class);
        when(mockOutboxEvent.getId()).thenReturn("event-1");
        when(mockOutboxEvent.getAggregateType()).thenReturn("type");
        when(mockOutboxEvent.getEventType()).thenReturn("event");
        when(mockOutboxEvent.getPayload()).thenReturn("payload");
        List<OutboxEvent> pendingEvents = Arrays.asList(mockOutboxEvent);
        when(outboxService.getPendingEvents()).thenReturn(pendingEvents);
        when(cloudEventFactory.createCloudEvent(anyString(), anyString(), anyString())).thenReturn("cloudEventJson");

        // Act
        outboxEventPublisher.processPendingEvents();

        // Assert
        verify(outboxService, times(1)).getPendingEvents();
        verify(cloudEventFactory, times(1)).createCloudEvent("type", "event", "payload");
        verify(kafkaEventProducer, times(1)).publishEvent("event", "cloudEventJson");
        verify(outboxService, times(1)).markEventAsProcessed("event-1");
        verify(outboxService, never()).markEventAsFailed(anyString(), anyString());
    }

    @Test
    void testProcessPendingEvents_NoPendingEvents() {
        // Arrange
        when(outboxService.getPendingEvents()).thenReturn(Collections.emptyList());

        // Act
        outboxEventPublisher.processPendingEvents();

        // Assert
        verify(outboxService, times(1)).getPendingEvents();
        verify(cloudEventFactory, never()).createCloudEvent(anyString(), anyString(), anyString());
        verify(kafkaEventProducer, never()).publishEvent(anyString(), anyString());
        verify(outboxService, never()).markEventAsProcessed(anyString());
        verify(outboxService, never()).markEventAsFailed(anyString(), anyString());
    }

    @Test
    void testProcessPendingEvents_CloudEventFactoryThrowsException() {
        // Arrange
        mockOutboxEvent = mock(OutboxEvent.class);
        when(mockOutboxEvent.getId()).thenReturn("event-1");
        when(mockOutboxEvent.getAggregateType()).thenReturn("type");
        when(mockOutboxEvent.getEventType()).thenReturn("event");
        when(mockOutboxEvent.getPayload()).thenReturn("payload");
        List<OutboxEvent> pendingEvents = Arrays.asList(mockOutboxEvent);
        when(outboxService.getPendingEvents()).thenReturn(pendingEvents);
        when(cloudEventFactory.createCloudEvent(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("CloudEvent creation failed"));

        // Act
        outboxEventPublisher.processPendingEvents();

        // Assert
        verify(outboxService, times(1)).getPendingEvents();
        verify(cloudEventFactory, times(1)).createCloudEvent("type", "event", "payload");
        verify(kafkaEventProducer, never()).publishEvent(anyString(), anyString());
        verify(outboxService, never()).markEventAsProcessed(anyString());
        verify(outboxService, times(1)).markEventAsFailed("event-1", "CloudEvent creation failed");
    }

    @Test
    void testProcessPendingEvents_KafkaEventProducerThrowsException() {
        // Arrange
        mockOutboxEvent = mock(OutboxEvent.class);
        when(mockOutboxEvent.getId()).thenReturn("event-1");
        when(mockOutboxEvent.getAggregateType()).thenReturn("type");
        when(mockOutboxEvent.getEventType()).thenReturn("event");
        when(mockOutboxEvent.getPayload()).thenReturn("payload");
        List<OutboxEvent> pendingEvents = Arrays.asList(mockOutboxEvent);
        when(outboxService.getPendingEvents()).thenReturn(pendingEvents);
        when(cloudEventFactory.createCloudEvent(anyString(), anyString(), anyString())).thenReturn("cloudEventJson");
        doThrow(new RuntimeException("Kafka publish failed")).when(kafkaEventProducer).publishEvent(anyString(), anyString());

        // Act
        outboxEventPublisher.processPendingEvents();

        // Assert
        verify(outboxService, times(1)).getPendingEvents();
        verify(cloudEventFactory, times(1)).createCloudEvent("type", "event", "payload");
        verify(kafkaEventProducer, times(1)).publishEvent("event", "cloudEventJson");
        verify(outboxService, never()).markEventAsProcessed(anyString());
        verify(outboxService, times(1)).markEventAsFailed("event-1", "Kafka publish failed");
    }
}
