package com.paklog.shipment.infrastructure.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.TraceContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;

class CloudEventSerializerTest {

    private final CloudEventSerializer serializer = new CloudEventSerializer(new ObjectMapper());
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void serializesCloudEventWithDataAndSubject() throws Exception {
        String json = serializer.serialize(
                "event-1",
                "shipment-1",
                "Shipment",
                "com.paklog.shipment.dispatched.v1",
                "{\"foo\":\"bar\"}",
                null
        );

        JsonNode node = mapper.readTree(json);
        assertEquals("event-1", node.get("id").asText());
        assertEquals("com.paklog.shipment.dispatched.v1", node.get("type").asText());
        assertEquals("urn:paklog:shipment-transportation/shipment", node.get("source").asText());
        assertEquals("shipment-1", node.get("subject").asText());
        assertEquals("application/json", node.get("datacontenttype").asText());
        assertTrue(node.hasNonNull("time"));
        assertTrue(node.has("data_base64"));
        byte[] decoded = Base64.getDecoder().decode(node.get("data_base64").asText());
        JsonNode payload = mapper.readTree(decoded);
        assertEquals("bar", payload.get("foo").asText());
    }

    @Test
    void includesTraceparentWhenTraceContextProvided() throws Exception {
        TraceContext traceContext = Mockito.mock(TraceContext.class);
        Mockito.when(traceContext.traceId()).thenReturn("traceId123");
        Mockito.when(traceContext.spanId()).thenReturn("spanId456");
        Mockito.when(traceContext.sampled()).thenReturn(true);

        String json = serializer.serialize(
                "event-2",
                null,
                "Shipment",
                "com.paklog.shipment.delivered.v1",
                null,
                traceContext
        );

        JsonNode node = mapper.readTree(json);
        assertEquals("00-traceId123-spanId456-01", node.get("traceparent").asText());
    }
}
