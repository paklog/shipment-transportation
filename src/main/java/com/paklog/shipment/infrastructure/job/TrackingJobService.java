package com.paklog.shipment.infrastructure.job;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.config.TrackingJobProperties;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.TrackingUpdate;
import com.paklog.shipment.domain.exception.CarrierException;
import com.paklog.shipment.domain.repository.ShipmentRepository;
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

    public TrackingJobService(
            ShipmentRepository shipmentRepository,
            ShipmentApplicationService shipmentApplicationService,
            List<ICarrierAdapter> carrierAdapterList,
            TrackingJobProperties trackingJobProperties) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentApplicationService = shipmentApplicationService;
        this.carrierAdapters = carrierAdapterList.stream()
                .collect(Collectors.toMap(
                        ICarrierAdapter::getCarrierName,
                        Function.identity()
                ));
        this.trackingJobProperties = trackingJobProperties;
    }

    @Scheduled(fixedRateString = "${tracking.job.interval:3600000}")
    public void updateTrackingStatus() {
        logger.info("Starting tracking update job");

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
                try {
                    boolean updated = updateShipmentTracking(shipment);
                    if (updated) {
                        successCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    logger.error("Failed to update tracking for shipment: {}",
                            shipment.getId(), e);
                }
            }

            lastSeenId = page.get(page.size() - 1).getId().toString();

            if (page.size() < batchSize) {
                break;
            }
        }

        logger.info("Tracking update job completed. Total: {}, Success: {}, Errors: {}",
                processed, successCount, errorCount);
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
}
