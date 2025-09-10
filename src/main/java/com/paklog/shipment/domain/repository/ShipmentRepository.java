package com.paklog.shipment.domain.repository;

import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.TrackingNumber;

import java.util.Optional;

public interface ShipmentRepository {
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(ShipmentId shipmentId);
    Optional<Shipment> findByTrackingNumber(TrackingNumber trackingNumber);
}
