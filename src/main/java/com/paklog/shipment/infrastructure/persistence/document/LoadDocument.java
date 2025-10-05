package com.paklog.shipment.infrastructure.persistence.document;

import com.paklog.shipment.domain.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Document(collection = "loads")
public class LoadDocument {

    @Id
    private String id;
    private String reference;
    private LoadStatus status;
    private CarrierName carrierName;
    private Set<String> shipmentIds;
    private LocationDocument origin;
    private LocationDocument destination;
    private LocalDate requestedPickupDate;
    private LocalDate requestedDeliveryDate;
    private PickupDocument pickup;
    private TenderDocument tender;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static LoadDocument fromDomain(Load load) {
        LoadDocument doc = new LoadDocument();
        doc.setId(load.getId().getValue().toString());
        doc.setReference(load.getReference());
        doc.setStatus(load.getStatus());
        doc.setCarrierName(load.getCarrierName());
        doc.setShipmentIds(load.getShipmentIds().stream().map(ShipmentId::getValue).map(Object::toString).collect(Collectors.toSet()));
        doc.setOrigin(LocationDocument.fromDomain(load.getOrigin()));
        doc.setDestination(LocationDocument.fromDomain(load.getDestination()));
        doc.setRequestedPickupDate(load.getRequestedPickupDate());
        doc.setRequestedDeliveryDate(load.getRequestedDeliveryDate());
        doc.setPickup(PickupDocument.fromDomain(load.getPickup()));
        doc.setTender(TenderDocument.fromDomain(load.getTender()));
        doc.setNotes(load.getNotes());
        doc.setCreatedAt(load.getCreatedAt());
        doc.setUpdatedAt(load.getUpdatedAt());
        return doc;
    }

    public Load toDomain() {
        Set<ShipmentId> domainShipmentIds = shipmentIds != null ? shipmentIds.stream().map(ShipmentId::of).collect(Collectors.toSet()) : new HashSet<>();
        return Load.restore(
                LoadId.of(this.id),
                this.reference,
                this.status,
                this.carrierName,
                domainShipmentIds,
                this.origin != null ? this.origin.toDomain() : null,
                this.destination != null ? this.destination.toDomain() : null,
                this.requestedPickupDate,
                this.requestedDeliveryDate,
                this.pickup != null ? this.pickup.toDomain() : null,
                this.tender != null ? this.tender.toDomain() : null,
                this.notes,
                this.createdAt,
                this.updatedAt
        );
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public LoadStatus getStatus() { return status; }
    public void setStatus(LoadStatus status) { this.status = status; }
    public CarrierName getCarrierName() { return carrierName; }
    public void setCarrierName(CarrierName carrierName) { this.carrierName = carrierName; }
    public Set<String> getShipmentIds() { return shipmentIds; }
    public void setShipmentIds(Set<String> shipmentIds) { this.shipmentIds = shipmentIds; }
    public LocationDocument getOrigin() { return origin; }
    public void setOrigin(LocationDocument origin) { this.origin = origin; }
    public LocationDocument getDestination() { return destination; }
    public void setDestination(LocationDocument destination) { this.destination = destination; }
    public LocalDate getRequestedPickupDate() { return requestedPickupDate; }
    public void setRequestedPickupDate(LocalDate requestedPickupDate) { this.requestedPickupDate = requestedPickupDate; }
    public LocalDate getRequestedDeliveryDate() { return requestedDeliveryDate; }
    public void setRequestedDeliveryDate(LocalDate requestedDeliveryDate) { this.requestedDeliveryDate = requestedDeliveryDate; }
    public PickupDocument getPickup() { return pickup; }
    public void setPickup(PickupDocument pickup) { this.pickup = pickup; }
    public TenderDocument getTender() { return tender; }
    public void setTender(TenderDocument tender) { this.tender = tender; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
