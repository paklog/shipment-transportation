package com.paklog.shipment.domain.repository;

import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.TrackingNumber;

import java.util.List;
import java.util.Optional;

public interface ShipmentRepository {
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(ShipmentId shipmentId);
    Optional<Shipment> findByOrderId(OrderId orderId);
    Optional<Shipment> findByTrackingNumber(TrackingNumber trackingNumber);
    List<Shipment> findPageInTransit(String lastSeenId, int limit);
    List<Shipment> findAll();
    void delete(ShipmentId shipmentId);
    boolean existsById(ShipmentId shipmentId);
    List<Shipment> findByLoadId(LoadId loadId);
    List<Shipment> findAllById(List<ShipmentId> shipmentIds);
}
