package com.paklog.shipment.application.port;

import com.paklog.shipment.domain.Shipment;

public interface ShipmentEventPublisher {

    void shipmentDispatched(Shipment shipment);

    void shipmentDelivered(Shipment shipment);
}
