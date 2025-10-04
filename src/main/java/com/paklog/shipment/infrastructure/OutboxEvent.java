package com.paklog.shipment.infrastructure;

import com.paklog.shipment.domain.DomainEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "outbox_events")
public class OutboxEvent {
    @Id
    private String id;
    private String aggregateId;
    private String aggregateType;
    private String eventType;
    private String payload;
    private String destination;
    private Instant createdAt;
    private Instant lastAttemptAt;
    private int attemptCount;
    private EventStatus status;
    private String errorMessage;

    public OutboxEvent() {}

    public OutboxEvent(DomainEvent event) {
        this(event.getAggregateId(), event.getAggregateType(), event.getEventType(), event.getDestination(), event.getPayload());
    }

    public OutboxEvent(String aggregateId, String aggregateType, String eventType, String destination, String payload) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.destination = destination;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.status = EventStatus.PENDING;
        this.attemptCount = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public String getAggregateType() { return aggregateType; }
    public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public int getAttemptCount() { return attemptCount; }
    public void incrementAttemptCount() { this.attemptCount++; }
    public void resetError() { this.errorMessage = null; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public boolean isProcessed() { return status == EventStatus.PROCESSED; }
    public void markProcessed() {
        this.status = EventStatus.PROCESSED;
        this.lastAttemptAt = Instant.now();
        incrementAttemptCount();
        resetError();
    }

    public void markForRetry(String errorMessage) {
        this.status = EventStatus.PENDING;
        this.lastAttemptAt = Instant.now();
        incrementAttemptCount();
        this.errorMessage = errorMessage;
    }

    public void markFailed(String errorMessage) {
        this.status = EventStatus.FAILED;
        this.lastAttemptAt = Instant.now();
        this.errorMessage = errorMessage;
    }

    public enum EventStatus {
        PENDING,
        PROCESSED,
        FAILED
    }
}
