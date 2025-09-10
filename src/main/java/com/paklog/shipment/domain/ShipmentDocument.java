package com.paklog.shipment.domain;

import java.util.List;
import java.util.stream.Collectors;

public class ShipmentDocument {
    private String id;
    private String orderId;
    private String trackingNumber;
    private String status;
    private List<TrackingEventDocument> trackingEvents;

    public static ShipmentDocument fromDomain(Shipment shipment) {
        ShipmentDocument doc = new ShipmentDocument();
        doc.setId(shipment.getId().getValue());
        doc.setOrderId(shipment.getOrderId().getValue());
        doc.setTrackingNumber(shipment.getTrackingNumber().getValue());
        doc.setStatus(shipment.getStatus().name());
        doc.setTrackingEvents(shipment.getTrackingEvents().stream()
                .map(TrackingEventDocument::fromDomain)
                .collect(Collectors.toList()));
        return doc;
    }

    public Shipment toDomain() {
        ShipmentId shipmentId = ShipmentId.of(id);
        OrderId orderIdObj = new OrderId(this.orderId);
        TrackingNumber trackingNumberObj = new TrackingNumber(this.trackingNumber);
        ShipmentStatus statusEnum = ShipmentStatus.valueOf(this.status);
        
        Shipment shipment = new Shipment(shipmentId, orderIdObj);
        shipment.assignTrackingNumber(trackingNumberObj);
        shipment.setStatus(statusEnum);
        
        this.trackingEvents.stream()
            .map(TrackingEventDocument::toDomain)
            .forEach(shipment::addTrackingEvent);
            
        return shipment;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<TrackingEventDocument> getTrackingEvents() { return trackingEvents; }
    public void setTrackingEvents(List<TrackingEventDocument> trackingEvents) { this.trackingEvents = trackingEvents; }
}