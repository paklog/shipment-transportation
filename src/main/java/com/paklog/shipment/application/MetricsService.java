package com.paklog.shipment.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final MeterRegistry registry;

    // Business Metrics
    public final Counter shipmentsCreated;
    public final Counter loadsCreated;
    public final Counter loadsTendered;
    public final Counter loadsBooked;

    // Infrastructure Metrics
    public final Counter kafkaEventsConsumed;
    public final Counter trackingJobsSucceeded;
    public final Counter trackingJobsFailed;
    private final Map<CarrierMetricKey, Counter> carrierApiCallCounters = new ConcurrentHashMap<>();
    private final Map<CarrierMetricKey, Timer> carrierApiLatencyTimers = new ConcurrentHashMap<>();

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
        this.kafkaEventsConsumed = Counter.builder("kafka.events.consumed")
                .description("Total Kafka events consumed")
                .register(registry);

        this.trackingJobsSucceeded = Counter.builder("tracking.jobs.succeeded")
                .description("Count of successful tracking job executions")
                .register(registry);

        this.trackingJobsFailed = Counter.builder("tracking.jobs.failed")
                .description("Count of failed tracking job executions")
                .register(registry);
    }

    public void incrementCarrierApiCalls(String carrier, String operation, String status) {
        recordCarrierApiCall(null, carrier, operation, status);
    }

    public Timer.Sample startCarrierApiTimer() {
        return Timer.start(registry);
    }

    public void recordCarrierApiCall(Timer.Sample sample, String carrier, String operation, String status) {
        CarrierMetricKey key = new CarrierMetricKey(carrier, operation, status);
        carrierApiCallCounters
                .computeIfAbsent(key, this::buildCarrierApiCounter)
                .increment();

        if (sample != null) {
            Timer timer = carrierApiLatencyTimers.computeIfAbsent(key, this::buildCarrierApiTimer);
            sample.stop(timer);
        }
    }

    public void markTrackingJobResult(boolean success) {
        if (success) {
            trackingJobsSucceeded.increment();
        } else {
            trackingJobsFailed.increment();
        }
    }

    private Counter buildCarrierApiCounter(CarrierMetricKey key) {
        return Counter.builder("carrier.api.calls")
                .description("Total calls to external carrier APIs")
                .tag("carrier", key.carrier)
                .tag("operation", key.operation)
                .tag("status", key.status)
                .register(registry);
    }

    private Timer buildCarrierApiTimer(CarrierMetricKey key) {
        return Timer.builder("carrier.api.latency")
                .description("Latency of external carrier API calls")
                .tag("carrier", key.carrier)
                .tag("operation", key.operation)
                .tag("status", key.status)
                .publishPercentileHistogram()
                .register(registry);
    }

    private record CarrierMetricKey(String carrier, String operation, String status) {
    }
}
