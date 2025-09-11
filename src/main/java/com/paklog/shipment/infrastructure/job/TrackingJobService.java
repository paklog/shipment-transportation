package com.paklog.shipment.infrastructure.job;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.TrackingEvent;
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
    private final Map<String, ICarrierAdapter> carrierAdapters;

    public TrackingJobService(
            ShipmentRepository shipmentRepository,
            ShipmentApplicationService shipmentApplicationService,
            List<ICarrierAdapter> carrierAdapterList) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentApplicationService = shipmentApplicationService;
        this.carrierAdapters = carrierAdapterList.stream()
                .collect(Collectors.toMap(
                        adapter -> adapter.getCarrierName().name(),
                        Function.identity()
                ));
    }

    @Scheduled(fixedRateString = "${tracking.job.interval:3600000}") // Default to 1 hour
    public void updateTrackingStatus() {
        logger.info("Starting tracking update job");

        List<Shipment> inTransitShipments = shipmentRepository.findAllInTransit();
        logger.info("Found {} shipments in transit", inTransitShipments.size());

        int successCount = 0;
        int errorCount = 0;

        for (Shipment shipment : inTransitShipments) {
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

        logger.info("Tracking update job completed. Success: {}, Errors: {}",
                successCount, errorCount);
    }

    private boolean updateShipmentTracking(Shipment shipment) {
        ICarrierAdapter carrier = carrierAdapters.get(shipment.getCarrierName().name());
        if (carrier == null) {
            logger.warn("No carrier adapter found for: {}", shipment.getCarrierName());
            return false;
        }

        try {
            Optional<TrackingUpdate> update = carrier.getTrackingStatus(shipment.getTrackingNumber());

            if (update.isPresent()) {
                TrackingUpdate trackingUpdate = update.get();
                List<TrackingEvent> newEvents = trackingUpdate.getNewEvents();

                if (!newEvents.isEmpty()) {
                    logger.debug("Found {} new tracking events for shipment: {}",
                            newEvents.size(), shipment.getId());

                    shipmentApplicationService.updateShipmentTracking(
                            shipment.getId(),
                            newEvents
                    );
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