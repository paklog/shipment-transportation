package com.paklog.shipment.infrastructure.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonFormat;
import io.micrometer.tracing.TraceContext;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CloudEventSerializer {

    private static final String SPEC_VERSION_SOURCE_PREFIX = "urn:paklog:shipment-transportation";
    private final ObjectMapper objectMapper;

    public CloudEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().registerModule(JsonFormat.getCloudEventJacksonModule());
    }

    public String serialize(String id,
                            String aggregateId,
                            String aggregateType,
                            String eventType,
                            String payload,
                            TraceContext traceContext) {
        var cloudEvent = buildCloudEvent(id, aggregateId, aggregateType, eventType, payload, traceContext);
        try {
            return objectMapper.writeValueAsString(cloudEvent);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialise CloudEvent", e);
        }
    }

    private io.cloudevents.CloudEvent buildCloudEvent(String id,
                                                     String aggregateId,
                                                     String aggregateType,
                                                     String eventType,
                                                     String data,
                                                     TraceContext traceContext) {
        String sourceType = aggregateType != null ? aggregateType.toLowerCase() : "unknown";

        CloudEventBuilder builder = CloudEventBuilder.v1()
                .withId(id != null ? id : UUID.randomUUID().toString())
                .withType(eventType)
                .withSource(URI.create(SPEC_VERSION_SOURCE_PREFIX + "/" + sourceType))
                .withTime(OffsetDateTime.now())
                .withDataContentType("application/json");

        if (aggregateId != null) {
            builder.withSubject(aggregateId);
        }

        if (data != null && !data.isBlank()) {
            builder.withData(data.getBytes(StandardCharsets.UTF_8));
        }

        if (traceContext != null) {
            builder.withExtension("traceparent", buildTraceParent(traceContext));
        }

        return builder.build();
    }

    private String buildTraceParent(TraceContext traceContext) {
        String sampledFlag = traceContext.sampled() ? "1" : "0";
        return "00-" + traceContext.traceId() + "-" + traceContext.spanId() + "-0" + sampledFlag;
    }

}
