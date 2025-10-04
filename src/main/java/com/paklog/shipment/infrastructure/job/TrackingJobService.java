package com.paklog.shipment.infrastructure.job;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.application.MetricsService;
import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.config.TrackingJobProperties;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.TrackingUpdate;
import com.paklog.shipment.domain.exception.CarrierException;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Scope;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TrackingJobService {
    private static final Logger logger = LoggerFactory.getLogger(TrackingJobService.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentApplicationService shipmentApplicationService;
    private final Map<CarrierName, ICarrierAdapter> carrierAdapters;
    private final TrackingJobProperties trackingJobProperties;
    private final ObservationRegistry observationRegistry;
    private final MetricsService metricsService;

    public TrackingJobService(
            ShipmentRepository shipmentRepository,
            ShipmentApplicationService shipmentApplicationService,
            List<ICarrierAdapter> carrierAdapterList,
            TrackingJobProperties trackingJobProperties,
            ObservationRegistry observationRegistry,
            MetricsService metricsService) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentApplicationService = shipmentApplicationService;
        this.carrierAdapters = carrierAdapterList.stream()
                .collect(Collectors.toMap(
                        ICarrierAdapter::getCarrierName,
                        Function.identity()
                ));
        this.trackingJobProperties = trackingJobProperties;
        this.observationRegistry = observationRegistry;
        this.metricsService = metricsService;
    }

    @Scheduled(fixedRateString = "${tracking.job.interval:3600000}")
    public void updateTrackingStatus() {
        logger.info("Starting tracking update job");
        Observation observation = Observation.createNotStarted("job.tracking.update", observationRegistry)
                .contextualName("trackingJob")
                .lowCardinalityKeyValue(KeyValue.of("job", "tracking-update"))
                .start();

        JobRunSummary summary = null;
        boolean success = false;
        try (Scope scope = observation.openScope()) {
            summary = executeTrackingUpdate();
            if (summary != null) {
                success = summary.errorCount() == 0;
                observation.lowCardinalityKeyValue(KeyValue.of("result", success ? "success" : "partial"));
                observation.highCardinalityKeyValue(KeyValue.of("shipments.processed", Integer.toString(summary.processed())));
            } else {
                observation.lowCardinalityKeyValue(KeyValue.of("result", "noop"));
            }
        } catch (RuntimeException ex) {
            observation.lowCardinalityKeyValue(KeyValue.of("result", "failed"));
            observation.error(ex);
            throw ex;
        } finally {
            observation.stop();
            boolean jobSuccess = summary == null || success;
            metricsService.markTrackingJobResult(jobSuccess);
            if (summary != null) {
                logger.info("Tracking update job completed. Total: {}, Success: {}, Errors: {}",
                        summary.processed(), summary.successCount(), summary.errorCount());
            } else {
                logger.info("Tracking update job completed. No in-transit shipments found.");
            }
        }
    }

    private boolean updateShipmentTracking(Shipment shipment) {
        ICarrierAdapter carrier = carrierAdapters.get(shipment.getCarrierName());
        if (carrier == null) {
            logger.warn("No carrier adapter found for: {}", shipment.getCarrierName());
            return false;
        }

        try {
            Optional<TrackingUpdate> update = carrier.getTrackingStatus(shipment.getTrackingNumber());

            if (update.isPresent()) {
                TrackingUpdate trackingUpdate = update.get();
                if (!trackingUpdate.getNewEvents().isEmpty() || trackingUpdate.isDelivered()) {
                    logger.debug("Applying tracking update for shipment {} (events: {}, delivered: {})",
                            shipment.getId(), trackingUpdate.getNewEvents().size(), trackingUpdate.isDelivered());

                    shipmentApplicationService.updateShipmentTracking(shipment.getId(), trackingUpdate);
                    return true;
                }
            }

        } catch (CarrierException e) {
            logger.warn("Carrier error updating tracking for shipment {}: {}",
                    shipment.getId(), e.getMessage());
        }

        return false;
    }

    private JobRunSummary executeTrackingUpdate() {
        String lastSeenId = null;
        int batchSize = trackingJobProperties.getBatchSize();
        int processed = 0;
        int successCount = 0;
        int errorCount = 0;

        while (true) {
            List<Shipment> page = shipmentRepository.findPageInTransit(lastSeenId, batchSize);
            if (page.isEmpty()) {
                break;
            }

            logger.info("Processing {} shipments in transit", page.size());

            for (Shipment shipment : page) {
                processed++;
                Observation shipmentObservation = Observation.createNotStarted("job.tracking.update.shipment", observationRegistry)
                        .contextualName("trackingUpdate")
                        .lowCardinalityKeyValue(KeyValue.of("carrier", carrierTag(shipment)))
                        .highCardinalityKeyValue(KeyValue.of("shipment.id", shipment.getId().toString()))
                        .start();
                try (Scope scope = shipmentObservation.openScope()) {
                    boolean updated = updateShipmentTracking(shipment);
                    if (updated) {
                        successCount++;
                        shipmentObservation.lowCardinalityKeyValue(KeyValue.of("status", "updated"));
                    } else {
                        shipmentObservation.lowCardinalityKeyValue(KeyValue.of("status", "noop"));
                    }
                } catch (Exception e) {
                    errorCount++;
                    shipmentObservation.lowCardinalityKeyValue(KeyValue.of("status", "error"));
                    shipmentObservation.error(e);
                    logger.error("Failed to update tracking for shipment: {}", shipment.getId(), e);
                } finally {
                    shipmentObservation.stop();
                }
            }

            lastSeenId = page.get(page.size() - 1).getId().toString();

            if (page.size() < batchSize) {
                break;
            }
        }

        if (processed == 0) {
            return null;
        }

        return new JobRunSummary(processed, successCount, errorCount);
    }

    private String carrierTag(Shipment shipment) {
        CarrierName carrierName = shipment.getCarrierName();
        return carrierName != null ? carrierName.name() : "UNKNOWN";
    }

    private record JobRunSummary(int processed, int successCount, int errorCount) {
    }
}
