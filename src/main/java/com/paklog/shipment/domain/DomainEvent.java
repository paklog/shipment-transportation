package com.paklog.shipment.domain;

public interface DomainEvent {
    String getAggregateId();
    String getAggregateType();
    String getEventType();
    String getDestination();
    String getPayload();
}
