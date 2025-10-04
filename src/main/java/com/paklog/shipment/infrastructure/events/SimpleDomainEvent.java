package com.paklog.shipment.infrastructure.events;

import com.paklog.shipment.domain.DomainEvent;

public class SimpleDomainEvent implements DomainEvent {

    private final String aggregateId;
    private final String aggregateType;
    private final String eventType;
    private final String destination;
    private final String payload;

    public SimpleDomainEvent(String aggregateId,
                             String aggregateType,
                             String eventType,
                             String destination,
                             String payload) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.destination = destination;
        this.payload = payload;
    }

    @Override
    public String getAggregateId() {
        return aggregateId;
    }

    @Override
    public String getAggregateType() {
        return aggregateType;
    }

    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public String getDestination() {
        return destination;
    }

    @Override
    public String getPayload() {
        return payload;
    }
}
