package com.paklog.shipment.infrastructure.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.shipment.infrastructure.OutboxEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;

class CloudEventFactoryTest {

    private final CloudEventFactory factory = new CloudEventFactory(new ObjectMapper());

    @Test
    void serializesCloudEventWithData() throws Exception {
        OutboxEvent event = new OutboxEvent("shipment-1", "Shipment", "event-type", "topic", "{\"foo\":\"bar\"}");
        event.setId("event-1");

        String json = factory.toCloudEventJson(event);

        JsonNode node = new ObjectMapper().readTree(json);
        assertEquals("1.0", node.get("specversion").asText());
        assertEquals("event-1", node.get("id").asText());
        assertEquals("event-type", node.get("type").asText());
        assertEquals("urn:paklog:shipment-transportation/shipment", node.get("source").asText());
        assertEquals("shipment-1", node.get("subject").asText());
        assertEquals("application/json", node.get("datacontenttype").asText());
        if (node.has("data")) {
            assertEquals("bar", node.get("data").get("foo").asText());
        } else {
            String base64 = node.get("data_base64").asText();
            byte[] decoded = Base64.getDecoder().decode(base64);
            JsonNode dataNode = new ObjectMapper().readTree(new String(decoded));
            assertEquals("bar", dataNode.get("foo").asText());
        }
        assertTrue(node.hasNonNull("time"));
    }

    @Test
    void handlesEmptyPayload() throws Exception {
        OutboxEvent event = new OutboxEvent("shipment-2", "Shipment", "event-type", "topic", null);

        String json = factory.toCloudEventJson(event);

        JsonNode node = new ObjectMapper().readTree(json);
        assertFalse(node.has("data"));
    }
}
