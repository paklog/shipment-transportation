package com.paklog.shipment.infrastructure;

import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.domain.events.PackagePackedCloudEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class PackagePackedEventConsumer {

    private final ShipmentApplicationService shipmentApplicationService;

    public PackagePackedEventConsumer(ShipmentApplicationService shipmentApplicationService) {
        this.shipmentApplicationService = shipmentApplicationService;
    }

    @Bean
    public Consumer<PackagePackedCloudEvent> packagePacked() {
        return event -> shipmentApplicationService.createShipmentFromPackage(event);
    }
}
