package com.paklog.shipment.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paklog.shipment.infrastructure.OutboxEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CloudEventFactory {

    private static final String SPEC_VERSION = "1.0";
    private static final String SOURCE_PREFIX = "urn:paklog:shipment-transportation";

    private final ObjectMapper objectMapper;

    public CloudEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toCloudEventJson(OutboxEvent event) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("specversion", SPEC_VERSION);
            root.put("id", event.getId() != null ? event.getId() : UUID.randomUUID().toString());
            root.put("type", event.getEventType());
            root.put("source", SOURCE_PREFIX + "/" + event.getAggregateType().toLowerCase());
            if (event.getAggregateId() != null) {
                root.put("subject", event.getAggregateId());
            }
            root.put("time", Instant.now().toString());
            root.put("datacontenttype", "application/json");

            if (event.getPayload() != null && !event.getPayload().isBlank()) {
                JsonNode dataNode = objectMapper.readTree(event.getPayload());
                root.set("data", dataNode);
            }

            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialise CloudEvent payload", e);
        }
    }
}
