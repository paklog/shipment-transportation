package com.paklog.shipment.infrastructure.job;

import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.domain.*;
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
import java.util.stream.Collectors;

@Service
public class AutomatedLoadBuildingJob {

    private static final Logger logger = LoggerFactory.getLogger(AutomatedLoadBuildingJob.class);
    private final ShipmentRepository shipmentRepository;
    private final LoadApplicationService loadApplicationService;
    private final ObservationRegistry observationRegistry;

    public AutomatedLoadBuildingJob(ShipmentRepository shipmentRepository,
                                    LoadApplicationService loadApplicationService,
                                    ObservationRegistry observationRegistry) {
        this.shipmentRepository = shipmentRepository;
        this.loadApplicationService = loadApplicationService;
        this.observationRegistry = observationRegistry;
    }

    @Scheduled(cron = "0 0 4 * * *") // Run every day at 4 AM
    public void buildLoads() {
        logger.info("Starting automated load building job.");
        Observation observation = Observation.createNotStarted("job.load.build", observationRegistry)
                .contextualName("automatedLoadBuilding")
                .start();
        int loadsCreated = 0;
        try (Scope scope = observation.openScope()) {
            List<LoadTemplate> templates = List.of(new LoadTemplate("New York", CarrierName.FEDEX));
            List<Shipment> unassignedShipments = shipmentRepository.findByLoadId(LoadId.of("00000000-0000-0000-0000-000000000000"));

            Map<String, List<Shipment>> shipmentsByDestination = unassignedShipments.stream()
                    .collect(Collectors.groupingBy(s -> "New York"));

            for (LoadTemplate template : templates) {
                List<Shipment> shipmentsForLoad = shipmentsByDestination.get(template.getDestinationCity());
                if (shipmentsForLoad != null && !shipmentsForLoad.isEmpty()) {
                    LoadId newLoadId = loadApplicationService.createLoad();
                    for (Shipment shipment : shipmentsForLoad) {
                        loadApplicationService.addShipmentToLoad(newLoadId, shipment.getId());
                    }
                    loadsCreated++;
                    logger.info("Created new load {} for destination {}", newLoadId, template.getDestinationCity());
                }
            }
            observation.lowCardinalityKeyValue(KeyValue.of("result", "success"));
        } catch (RuntimeException ex) {
            observation.lowCardinalityKeyValue(KeyValue.of("result", "failed"));
            observation.error(ex);
            throw ex;
        } finally {
            observation.highCardinalityKeyValue(KeyValue.of("loads.created", Integer.toString(loadsCreated)));
            observation.stop();
            logger.info("Automated load building job finished. Loads created: {}", loadsCreated);
        }
    }
}
