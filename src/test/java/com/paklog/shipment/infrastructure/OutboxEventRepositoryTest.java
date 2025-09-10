package com.paklog.shipment.infrastructure;

import com.paklog.shipment.domain.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataMongoTest
@Testcontainers
class OutboxEventRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:latest"));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void testFindByStatusPending() {
        // Arrange
        DomainEvent mockDomainEvent1 = mock(DomainEvent.class);
        when(mockDomainEvent1.getAggregateId()).thenReturn("agg-1");
        when(mockDomainEvent1.getAggregateType()).thenReturn("type-1");
        when(mockDomainEvent1.getEventType()).thenReturn("event-1");
        when(mockDomainEvent1.getPayload()).thenReturn("{}");

        DomainEvent mockDomainEvent2 = mock(DomainEvent.class);
        when(mockDomainEvent2.getAggregateId()).thenReturn("agg-2");
        when(mockDomainEvent2.getAggregateType()).thenReturn("type-2");
        when(mockDomainEvent2.getEventType()).thenReturn("event-2");
        when(mockDomainEvent2.getPayload()).thenReturn("{}");

        OutboxEvent event1 = new OutboxEvent(mockDomainEvent1);
        event1.setStatus(OutboxEvent.EventStatus.PENDING);

        OutboxEvent event2 = new OutboxEvent(mockDomainEvent2);
        event2.setStatus(OutboxEvent.EventStatus.PROCESSED);

        outboxEventRepository.save(event1);
        outboxEventRepository.save(event2);

        // Act
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(OutboxEvent.EventStatus.PENDING);

        // Assert
        assertEquals(1, pendingEvents.size());
        assertEquals("agg-1", pendingEvents.get(0).getAggregateId());
        assertFalse(pendingEvents.get(0).isProcessed());
    }
}
