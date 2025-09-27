package com.paklog.shipment.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MetricsService {

    private final MeterRegistry registry;

    // Business Metrics
    public final Counter shipmentsCreated;
    public final Counter loadsCreated;
    public final Counter loadsTendered;
    public final Counter loadsBooked;

    // Infrastructure Metrics
    private final Counter carrierApiCalls;
    public final Counter kafkaEventsConsumed;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
        // Business Metrics
        this.shipmentsCreated = Counter.builder("shipments.created")
                .description("Total number of shipments created")
                .register(registry);

        this.loadsCreated = Counter.builder("loads.created")
                .description("Total number of loads created")
                .register(registry);

        this.loadsTendered = Counter.builder("loads.tendered")
                .description("Total number of loads tendered to a carrier")
                .register(registry);

        this.loadsBooked = Counter.builder("loads.booked")
                .description("Total number of loads booked by a carrier")
                .register(registry);

        // Infrastructure Metrics
        this.carrierApiCalls = Counter.builder("carrier.api.calls")
                .description("Total calls to external carrier APIs")
                .register(registry);

        this.kafkaEventsConsumed = Counter.builder("kafka.events.consumed")
                .description("Total Kafka events consumed")
                .register(registry);
    }

    public void incrementCarrierApiCalls(String carrier, String operation, String status) {
        Counter.builder("carrier.api.calls")
                .description("Total calls to external carrier APIs")
                .tag("carrier", carrier)
                .tag("operation", operation)
                .tag("status", status)
                .register(registry)
                .increment();
    }
}
