package com.paklog.shipment.infrastructure.api;

import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.TrackingEvent;
import com.paklog.shipment.infrastructure.api.dto.LocationResponse;
import com.paklog.shipment.infrastructure.api.dto.ShipmentResponse;
import com.paklog.shipment.infrastructure.api.dto.TrackingEventResponse;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class ShipmentResponseMapper {

    private ShipmentResponseMapper() {
    }

    public static ShipmentResponse toResponse(Shipment shipment) {
        ShipmentResponse response = new ShipmentResponse();
        response.setShipmentId(shipment.getId().getValue().toString());
        response.setOrderId(shipment.getOrderId().getValue().toString());
        response.setCarrierName(humanizeCarrierName(shipment));
        response.setStatus(shipment.getStatus().name().toLowerCase(Locale.ROOT));
        response.setTrackingNumber(shipment.getTrackingNumber() != null ? shipment.getTrackingNumber().getValue() : null);
        response.setCreatedAt(shipment.getCreatedAt());
        response.setDispatchedAt(shipment.getDispatchedAt());
        response.setDeliveredAt(shipment.getDeliveredAt());
        response.setTrackingHistory(mapTrackingHistory(shipment.getTrackingEvents()));
        return response;
    }

    private static String humanizeCarrierName(Shipment shipment) {
        String name = shipment.getCarrierName().name().toLowerCase(Locale.ROOT);
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    private static List<TrackingEventResponse> mapTrackingHistory(List<TrackingEvent> events) {
        return events.stream().map(ShipmentResponseMapper::mapEvent).collect(Collectors.toList());
    }

    private static TrackingEventResponse mapEvent(TrackingEvent event) {
        TrackingEventResponse response = new TrackingEventResponse();
        response.setStatus(event.getStatus());
        response.setStatusDescription(event.getStatusDescription());
        response.setTimestamp(event.getTimestamp());
        response.setEventCode(event.getEventCode());
        response.setDetailedDescription(event.getDetailedDescription());
        response.setLocation(new LocationResponse(event.getLocation(), null, null, null));
        return response;
    }
}
