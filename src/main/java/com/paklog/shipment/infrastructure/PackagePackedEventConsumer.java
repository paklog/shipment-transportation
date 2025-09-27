package com.paklog.shipment.infrastructure;

import com.paklog.shipment.application.MetricsService;
import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.application.command.CreateShipmentCommand;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.events.PackagePackedCloudEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class PackagePackedEventConsumer {

    private final ShipmentApplicationService shipmentApplicationService;
    private final MetricsService metricsService;

    public PackagePackedEventConsumer(ShipmentApplicationService shipmentApplicationService, MetricsService metricsService) {
        this.shipmentApplicationService = shipmentApplicationService;
        this.metricsService = metricsService;
    }

    @Bean
    public Consumer<PackagePackedCloudEvent> packagePacked() {
        return event -> {
            metricsService.kafkaEventsConsumed.increment(); // Increment metric
            CreateShipmentCommand command = new CreateShipmentCommand(event.getPackageId(), event.getOrderId());
            shipmentApplicationService.createShipment(command);
        };
    }
}
