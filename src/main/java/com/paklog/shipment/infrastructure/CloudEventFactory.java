package com.paklog.shipment.infrastructure;

import org.springframework.stereotype.Component;

@Component
public class CloudEventFactory {
    public String createCloudEvent(String aggregateType, String eventType, String payload) {
        // TODO: Implement actual logic to create a cloud event
        return "{\"aggregateType\":\"" + aggregateType + "\",\"eventType\":\"" + eventType + "\",\"payload\":" + payload + "}";
    }
}