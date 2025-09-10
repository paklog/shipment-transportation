package com.paklog.shipment.infrastructure;

import com.paklog.shipment.domain.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaEventProducer kafkaEventProducer;

    @InjectMocks
    private OutboxService outboxService;

    private DomainEvent mockDomainEvent;
    private OutboxEvent mockOutboxEvent;

    @BeforeEach
    void setUp() {
        mockDomainEvent = mock(DomainEvent.class);
        when(mockDomainEvent.getAggregateId()).thenReturn("agg-1");
        when(mockDomainEvent.getAggregateType()).thenReturn("type-1");
        when(mockDomainEvent.getEventType()).thenReturn("event-1");
        when(mockDomainEvent.getPayload()).thenReturn("{}");

        mockOutboxEvent = new OutboxEvent(mockDomainEvent);
        mockOutboxEvent.setId("outbox-1");
    }

    @Test
    void testSave() {
        // Arrange
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(mockOutboxEvent);

        // Act
        OutboxEvent savedEvent = outboxService.save(mockDomainEvent);

        // Assert
        assertNotNull(savedEvent);
        assertEquals("outbox-1", savedEvent.getId());
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
    }

    @Test
    void testGetPendingEvents() {
        // Arrange
        List<OutboxEvent> pendingEvents = Arrays.asList(mockOutboxEvent);
        when(outboxEventRepository.findByStatus(OutboxEvent.EventStatus.PENDING)).thenReturn(pendingEvents);

        // Act
        List<OutboxEvent> result = outboxService.getPendingEvents();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(mockOutboxEvent, result.get(0));
        verify(outboxEventRepository, times(1)).findByStatus(OutboxEvent.EventStatus.PENDING);
    }

    @Test
    void testMarkEventAsProcessed() {
        // Arrange
        when(outboxEventRepository.findById("outbox-1")).thenReturn(Optional.of(mockOutboxEvent));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(mockOutboxEvent);

        // Act
        outboxService.markEventAsProcessed("outbox-1");

        // Assert
        assertTrue(mockOutboxEvent.isProcessed());
        verify(outboxEventRepository, times(1)).findById("outbox-1");
        verify(outboxEventRepository, times(1)).save(mockOutboxEvent);
    }

    @Test
    void testMarkEventAsFailed() {
        // Arrange
        String errorMessage = "Test error";
        when(outboxEventRepository.findById("outbox-1")).thenReturn(Optional.of(mockOutboxEvent));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(mockOutboxEvent);

        // Act
        outboxService.markEventAsFailed("outbox-1", errorMessage);

        // Assert
        assertFalse(mockOutboxEvent.isProcessed());
        assertEquals(errorMessage, mockOutboxEvent.getErrorMessage());
        verify(outboxEventRepository, times(1)).findById("outbox-1");
        verify(outboxEventRepository, times(1)).save(mockOutboxEvent);
    }
}
