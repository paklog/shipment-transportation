package com.paklog.shipment.domain;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository {
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(ShipmentId id);
    List<Shipment> findByOrderId(OrderId orderId);
    List<Shipment> findAllInTransit();
}