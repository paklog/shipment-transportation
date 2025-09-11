package com.paklog.shipment.infrastructure.job;

import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.domain.*;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AutomatedLoadBuildingJob {

    private static final Logger logger = LoggerFactory.getLogger(AutomatedLoadBuildingJob.class);
    private final ShipmentRepository shipmentRepository;
    private final LoadApplicationService loadApplicationService;

    public AutomatedLoadBuildingJob(ShipmentRepository shipmentRepository, LoadApplicationService loadApplicationService) {
        this.shipmentRepository = shipmentRepository;
        this.loadApplicationService = loadApplicationService;
    }

    @Scheduled(cron = "0 0 4 * * *") // Run every day at 4 AM
    public void buildLoads() {
        logger.info("Starting automated load building job.");

        // 1. Get all templates (mocked for now)
        List<LoadTemplate> templates = List.of(new LoadTemplate("New York", CarrierName.FEDEX));

        // 2. Get all unassigned shipments
        List<Shipment> unassignedShipments = shipmentRepository.findByLoadId(LoadId.of("00000000-0000-0000-0000-000000000000"));

        // 3. Group shipments by destination (mocked for now)
        Map<String, List<Shipment>> shipmentsByDestination = unassignedShipments.stream()
                .collect(Collectors.groupingBy(s -> "New York")); // Mock destination

        // 4. Create loads based on templates
        for (LoadTemplate template : templates) {
            List<Shipment> shipmentsForLoad = shipmentsByDestination.get(template.getDestinationCity());
            if (shipmentsForLoad != null && !shipmentsForLoad.isEmpty()) {
                LoadId newLoadId = loadApplicationService.createLoad();
                for (Shipment shipment : shipmentsForLoad) {
                    loadApplicationService.addShipmentToLoad(newLoadId, shipment.getId());
                }
                logger.info("Created new load {} for destination {}", newLoadId, template.getDestinationCity());
            }
        }
        logger.info("Automated load building job finished.");
    }
}
