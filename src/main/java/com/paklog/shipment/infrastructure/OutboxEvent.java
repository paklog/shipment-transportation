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
    private Instant createdAt;
    private EventStatus status;
    private String errorMessage;

    public OutboxEvent() {}

    public OutboxEvent(DomainEvent event) {
        this.aggregateId = event.getAggregateId();
        this.aggregateType = event.getAggregateType();
        this.eventType = event.getEventType();
        this.payload = event.getPayload();
        this.createdAt = Instant.now();
        this.status = EventStatus.PENDING;
    }

    public OutboxEvent(String aggregateId, String aggregateType, String eventType, String payload) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
        this.status = EventStatus.PENDING;
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
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public boolean isProcessed() { return status == EventStatus.PROCESSED; }
    public void setProcessed(boolean processed) { this.status = processed ? EventStatus.PROCESSED : EventStatus.PENDING; }

    public enum EventStatus {
        PENDING,
        PROCESSED,
        FAILED
    }
}