package com.paklog.shipment.infrastructure;

import com.paklog.shipment.application.MetricsService;
import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.application.command.CreateShipmentCommand;
import com.paklog.shipment.domain.events.PackagePackedCloudEvent;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class PackagePackedEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PackagePackedEventConsumer.class);

    private final ShipmentApplicationService shipmentApplicationService;
    private final MetricsService metricsService;
    private final ObservationRegistry observationRegistry;

    public PackagePackedEventConsumer(ShipmentApplicationService shipmentApplicationService,
                                      MetricsService metricsService,
                                      ObservationRegistry observationRegistry) {
        this.shipmentApplicationService = shipmentApplicationService;
        this.metricsService = metricsService;
        this.observationRegistry = observationRegistry;
    }

    @Bean
    public Consumer<PackagePackedCloudEvent> packagePacked() {
        return event -> {
            Observation.createNotStarted("event.package-packed.consume", observationRegistry)
                    .contextualName("packagePackedEvent")
                    .lowCardinalityKeyValue(KeyValue.of("event.type", "package.packed"))
                    .highCardinalityKeyValue(KeyValue.of("package.id", event.getPackageId()))
                    .observe(() -> {
                        metricsService.kafkaEventsConsumed.increment();
                        try {
                            CreateShipmentCommand command = new CreateShipmentCommand(event.getPackageId(), event.getOrderId());
                            shipmentApplicationService.createShipment(command);
                            logger.debug("Processed PackagePacked event for package {}", event.getPackageId());
                        } catch (Exception ex) {
                            logger.error("Failed to process PackagePacked event for package {}", event.getPackageId(), ex);
                            throw ex;
                        }
                    });
        };
    }
}
