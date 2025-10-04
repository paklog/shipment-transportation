package com.paklog.shipment.infrastructure.events;

import com.paklog.shipment.infrastructure.KafkaEventProducer;
import com.paklog.shipment.infrastructure.OutboxEvent;
import com.paklog.shipment.infrastructure.OutboxService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private KafkaEventProducer kafkaEventProducer;

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private CloudEventFactory cloudEventFactory;

    @Test
    void publishesPendingEventsAndMarksProcessed() {
        OutboxEvent event = new OutboxEvent("agg-1", "Shipment", "type", "topic", "{}");
        event.setId("event-1");
        when(outboxService.getPendingEvents()).thenReturn(List.of(event));
        when(cloudEventFactory.toCloudEventJson(event)).thenReturn("cloud-event-json");

        outboxEventPublisher.processPendingEvents();

        verify(kafkaEventProducer).publishEvent("topic", "cloud-event-json");
        verify(outboxService).markEventAsProcessed("event-1");
        verify(outboxService, never()).markEventAsFailed(anyString(), anyString());
    }

    @Test
    void marksEventForRetryWhenPublishingFails() {
        OutboxEvent event = new OutboxEvent("agg-1", "Shipment", "type", "topic", "{}");
        event.setId("event-1");
        when(outboxService.getPendingEvents()).thenReturn(List.of(event));
        when(cloudEventFactory.toCloudEventJson(event)).thenReturn("cloud-event-json");
        doThrow(new KafkaEventProducer.EventPublishingException("failure", new RuntimeException()))
                .when(kafkaEventProducer).publishEvent(anyString(), anyString());

        outboxEventPublisher.processPendingEvents();

        verify(outboxService).markEventAsFailed(eq("event-1"), anyString());
        verify(outboxService, never()).markEventAsProcessed(anyString());
    }

    @Test
    void skipsWhenNoPendingEvents() {
        when(outboxService.getPendingEvents()).thenReturn(List.of());

        outboxEventPublisher.processPendingEvents();

        verifyNoInteractions(cloudEventFactory, kafkaEventProducer);
    }
}
