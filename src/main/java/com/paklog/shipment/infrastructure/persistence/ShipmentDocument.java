package com.paklog.shipment.infrastructure.persistence;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.domain.TrackingEvent;
import com.paklog.shipment.domain.TrackingNumber;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Document(collection = "shipments")
public class ShipmentDocument {
    @Id
    private String id;
    private String orderId;
    private String carrierName;
    private String trackingNumber;
    private String status;
    private Instant createdAt;
    private Instant dispatchedAt;
    private Instant deliveredAt;
    private List<TrackingEventDocument> trackingEvents;
    private byte[] labelData;

    public static ShipmentDocument fromDomain(Shipment shipment) {
        ShipmentDocument doc = new ShipmentDocument();
        doc.setId(shipment.getId().getValue().toString());
        doc.setOrderId(shipment.getOrderId().getValue().toString());
        doc.setCarrierName(shipment.getCarrierName().name());
        doc.setTrackingNumber(shipment.getTrackingNumber() != null ? shipment.getTrackingNumber().getValue() : null);
        doc.setStatus(shipment.getStatus().name());
        doc.setCreatedAt(shipment.getCreatedAt());
        doc.setDispatchedAt(shipment.getDispatchedAt());
        doc.setDeliveredAt(shipment.getDeliveredAt());
        doc.setLabelData(shipment.getLabelData());
        doc.setTrackingEvents(shipment.getTrackingEvents().stream()
            .map(TrackingEventDocument::fromDomain)
            .collect(Collectors.toList()));
        return doc;
    }

    public Shipment toDomain() {
        List<TrackingEvent> events = trackingEvents == null
            ? List.of()
            : trackingEvents.stream().map(TrackingEventDocument::toDomain).collect(Collectors.toList());

        return Shipment.restore(
            ShipmentId.of(id),
            OrderId.of(orderId),
            CarrierName.valueOf(carrierName),
            trackingNumber != null ? TrackingNumber.of(trackingNumber) : null,
            labelData,
            ShipmentStatus.valueOf(status),
            createdAt,
            dispatchedAt,
            deliveredAt,
            events
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(Instant dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public List<TrackingEventDocument> getTrackingEvents() {
        return trackingEvents;
    }

    public void setTrackingEvents(List<TrackingEventDocument> trackingEvents) {
        this.trackingEvents = trackingEvents;
    }

    public byte[] getLabelData() {
        return labelData;
    }

    public void setLabelData(byte[] labelData) {
        this.labelData = labelData;
    }
}
