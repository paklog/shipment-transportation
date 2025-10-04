package com.paklog.shipment.infrastructure.events;

import com.paklog.shipment.infrastructure.KafkaEventProducer;
import com.paklog.shipment.infrastructure.OutboxEvent;
import com.paklog.shipment.infrastructure.OutboxService;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private KafkaEventProducer kafkaEventProducer;

    @Mock
    private CloudEventSerializer cloudEventSerializer;

    @Mock
    private Tracer tracer;

    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void setUp() {
        when(tracer.currentSpan()).thenReturn(null);
        outboxEventPublisher = new OutboxEventPublisher(
                outboxService,
                cloudEventSerializer,
                kafkaEventProducer,
                ObservationRegistry.create(),
                tracer
        );
    }

    @Test
    void publishesPendingEventsAndMarksProcessed() {
        OutboxEvent event = new OutboxEvent("agg-1", "Shipment", "type", "topic", "{}");
        event.setId("event-1");
        when(outboxService.getPendingEvents()).thenReturn(List.of(event));
        when(cloudEventSerializer.serialize(any(), any(), any(), any(), any(), any())).thenReturn("cloud-event-json");

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
        when(cloudEventSerializer.serialize(any(), any(), any(), any(), any(), any())).thenReturn("cloud-event-json");
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

        verifyNoInteractions(cloudEventSerializer, kafkaEventProducer);
    }
}
