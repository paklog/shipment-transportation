package com.paklog.shipment.infrastructure.api.mapper;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.domain.TrackingEvent;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.infrastructure.api.gen.dto.OrderId;
import com.paklog.shipment.infrastructure.api.gen.dto.ShipmentCollection;
import com.paklog.shipment.infrastructure.api.gen.dto.ShipmentId;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ShipmentMapper {

    public com.paklog.shipment.infrastructure.api.gen.dto.Shipment toDto(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment");

        var dto = new com.paklog.shipment.infrastructure.api.gen.dto.Shipment();
        dto.setId(toDto(shipment.getId()));
        dto.setOrderId(toDto(shipment.getOrderId()));
        dto.setStatus(toDto(shipment.getStatus()));
        dto.setCarrierName(toDto(shipment.getCarrierName()));
        if (shipment.getTrackingNumber() != null) {
            dto.setTrackingNumber(toDto(shipment.getTrackingNumber()));
        }
        dto.setTrackingEvents(shipment.getTrackingEvents().stream().map(this::toDto).toList());
        dto.setAssignedLoadId(shipment.getAssignedLoadId() != null ? shipment.getAssignedLoadId().getValue() : null);
        dto.setLastUpdatedAt(shipment.getLastUpdatedAt());
        return dto;
    }

    public ShipmentCollection toDto(Page<Shipment> page) {
        var collection = new ShipmentCollection();
        collection.setItems(page.getContent().stream().map(this::toDto).toList());
        collection.setPage(page.getNumber());
        collection.setSize(page.getSize());
        collection.setTotalItems(Math.toIntExact(page.getTotalElements()));
        collection.setTotalPages(page.getTotalPages());
        return collection;
    }

    public ShipmentStatus toDomain(com.paklog.shipment.infrastructure.api.gen.dto.ShipmentStatus status) {
        return status != null ? ShipmentStatus.valueOf(status.name()) : null;
    }

    public CarrierName toDomain(com.paklog.shipment.infrastructure.api.gen.dto.CarrierName carrierName) {
        return carrierName != null ? CarrierName.valueOf(carrierName.name()) : null;
    }

    private com.paklog.shipment.infrastructure.api.gen.dto.ShipmentStatus toDto(ShipmentStatus status) {
        return status != null ? com.paklog.shipment.infrastructure.api.gen.dto.ShipmentStatus.valueOf(status.name()) : null;
    }

    private com.paklog.shipment.infrastructure.api.gen.dto.CarrierName toDto(CarrierName carrierName) {
        return carrierName != null ? com.paklog.shipment.infrastructure.api.gen.dto.CarrierName.valueOf(carrierName.name()) : null;
    }

    private com.paklog.shipment.infrastructure.api.gen.dto.TrackingEvent toDto(TrackingEvent event) {
        var dto = new com.paklog.shipment.infrastructure.api.gen.dto.TrackingEvent();
        dto.setStatus(event.getStatus());
        dto.setStatusDescription(event.getStatusDescription());
        dto.setLocation(event.getLocation());
        dto.setTimestamp(event.getTimestamp());
        dto.setEventCode(event.getEventCode());
        dto.setDetailedDescription(event.getDetailedDescription());
        return dto;
    }

    private com.paklog.shipment.infrastructure.api.gen.dto.TrackingNumber toDto(TrackingNumber trackingNumber) {
        var dto = new com.paklog.shipment.infrastructure.api.gen.dto.TrackingNumber();
        dto.setValue(trackingNumber.getValue());
        return dto;
    }

    private ShipmentId toDto(com.paklog.shipment.domain.ShipmentId shipmentId) {
        var dto = new ShipmentId();
        dto.setValue(shipmentId.getValue());
        return dto;
    }

    private OrderId toDto(com.paklog.shipment.domain.OrderId orderId) {
        var dto = new OrderId();
        dto.setValue(orderId.getValue());
        return dto;
    }
}
