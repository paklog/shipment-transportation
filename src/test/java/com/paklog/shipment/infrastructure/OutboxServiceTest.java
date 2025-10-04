package com.paklog.shipment.infrastructure;

import com.paklog.shipment.config.OutboxProperties;
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

    private OutboxService outboxService;
    private OutboxProperties outboxProperties;

    private DomainEvent mockDomainEvent;
    private OutboxEvent mockOutboxEvent;

    @BeforeEach
    void setUp() {
        outboxProperties = new OutboxProperties();
        mockDomainEvent = mock(DomainEvent.class);
        when(mockDomainEvent.getAggregateId()).thenReturn("agg-1");
        when(mockDomainEvent.getAggregateType()).thenReturn("type-1");
        when(mockDomainEvent.getEventType()).thenReturn("event-1");
        when(mockDomainEvent.getDestination()).thenReturn("topic-1");
        when(mockDomainEvent.getPayload()).thenReturn("{}");

        mockOutboxEvent = new OutboxEvent(mockDomainEvent);
        mockOutboxEvent.setId("outbox-1");

        outboxService = new OutboxService(outboxEventRepository, outboxProperties);
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
        when(outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING)).thenReturn(pendingEvents);

        // Act
        List<OutboxEvent> result = outboxService.getPendingEvents();

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(mockOutboxEvent, result.get(0));
        verify(outboxEventRepository, times(1)).findTop100ByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING);
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
        assertEquals(1, mockOutboxEvent.getAttemptCount());
        assertNotNull(mockOutboxEvent.getLastAttemptAt());
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
        assertEquals(1, mockOutboxEvent.getAttemptCount());
        assertNotNull(mockOutboxEvent.getLastAttemptAt());
        verify(outboxEventRepository, times(1)).findById("outbox-1");
        verify(outboxEventRepository, times(1)).save(mockOutboxEvent);
    }

    @Test
    void testMarkEventAsFailedMarksFinalAfterMaxAttempts() {
        outboxProperties.setMaxAttempts(3);
        // Simulate two prior failures
        mockOutboxEvent.markForRetry("prev1");
        mockOutboxEvent.markForRetry("prev2");

        when(outboxEventRepository.findById("outbox-1")).thenReturn(Optional.of(mockOutboxEvent));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenReturn(mockOutboxEvent);

        outboxService.markEventAsFailed("outbox-1", "final-error");

        assertEquals(OutboxEvent.EventStatus.FAILED, mockOutboxEvent.getStatus());
        assertEquals("final-error", mockOutboxEvent.getErrorMessage());
        assertEquals(3, mockOutboxEvent.getAttemptCount());
    }
}
