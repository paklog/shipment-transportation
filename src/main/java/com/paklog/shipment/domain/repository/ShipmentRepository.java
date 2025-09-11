package com.paklog.shipment.domain.repository;

import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository {
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(ShipmentId shipmentId);
    Optional<Shipment> findByOrderId(OrderId orderId);
    List<Shipment> findAllInTransit();
    List<Shipment> findAll();
    void delete(ShipmentId shipmentId);
    boolean existsById(ShipmentId shipmentId);
    List<Shipment> findByLoadId(LoadId loadId);
}