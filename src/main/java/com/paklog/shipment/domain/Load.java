package com.paklog.shipment.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Load {

    private final LoadId id;
    private String reference;
    private LoadStatus status;
    private CarrierName carrierName;
    private final Set<ShipmentId> shipmentIds;
    private Location origin;
    private Location destination;
    private LocalDate requestedPickupDate;
    private LocalDate requestedDeliveryDate;
    private Pickup pickup;
    private Tender tender;
    private String notes;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Load(String reference, Set<ShipmentId> shipments, Location origin, Location destination, LocalDate requestedPickupDate, LocalDate requestedDeliveryDate, String notes) {
        this.id = LoadId.generate();
        this.reference = Objects.requireNonNull(reference);
        this.shipmentIds = new HashSet<>(Objects.requireNonNull(shipments));
        this.origin = Objects.requireNonNull(origin);
        this.destination = Objects.requireNonNull(destination);
        this.requestedPickupDate = Objects.requireNonNull(requestedPickupDate);
        this.requestedDeliveryDate = Objects.requireNonNull(requestedDeliveryDate);
        this.notes = notes;
        this.status = LoadStatus.PLANNED;
        this.tender = Tender.notTendered();
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;

        if (shipments.isEmpty()) {
            throw new IllegalArgumentException("A load must contain at least one shipment.");
        }
    }

    // Restore factory method for persistence
    public static Load restore(
            LoadId id, String reference, LoadStatus status, CarrierName carrierName, Set<ShipmentId> shipmentIds,
            Location origin, Location destination, LocalDate requestedPickupDate, LocalDate requestedDeliveryDate,
            Pickup pickup, Tender tender, String notes, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        return new Load(id, reference, status, carrierName, shipmentIds, origin, destination, requestedPickupDate,
                requestedDeliveryDate, pickup, tender, notes, createdAt, updatedAt);
    }
    
    // Private constructor for the restore method
    private Load(
            LoadId id, String reference, LoadStatus status, CarrierName carrierName, Set<ShipmentId> shipmentIds,
            Location origin, Location destination, LocalDate requestedPickupDate, LocalDate requestedDeliveryDate,
            Pickup pickup, Tender tender, String notes, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.reference = reference;
        this.status = status;
        this.carrierName = carrierName;
        this.shipmentIds = shipmentIds;
        this.origin = origin;
        this.destination = destination;
        this.requestedPickupDate = requestedPickupDate;
        this.requestedDeliveryDate = requestedDeliveryDate;
        this.pickup = pickup;
        this.tender = tender;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


        public void applyUpdate(String reference, LocalDate requestedPickupDate, LocalDate requestedDeliveryDate, LoadStatus status, String notes) {
        if (reference != null) {
            this.reference = reference;
        }
        if (requestedPickupDate != null) {
            this.requestedPickupDate = requestedPickupDate;
        }
        if (requestedDeliveryDate != null) {
            this.requestedDeliveryDate = requestedDeliveryDate;
        }
        if (status != null) {
            this.status = status;
        }
        if (notes != null) {
            this.notes = notes;
        }
        this.updatedAt = OffsetDateTime.now();
    }


        public void addShipments(Set<ShipmentId> newShipmentIds) {
        if (this.status != LoadStatus.PLANNED) {
            throw new IllegalStateException("Cannot add shipments to a load that is not in PLANNED state.");
        }
        // Check for duplicates
        for (ShipmentId newId : newShipmentIds) {
            if (this.shipmentIds.contains(newId)) {
                throw new IllegalArgumentException("Shipment " + newId.getValue() + " is already assigned to this load.");
            }
        }
        this.shipmentIds.addAll(newShipmentIds);
        this.updatedAt = OffsetDateTime.now();
    }


        public void removeShipment(ShipmentId shipmentId) {
        if (this.status != LoadStatus.PLANNED) {
            throw new IllegalStateException("Cannot remove shipments from a load that is not in PLANNED state.");
        }
        if (!this.shipmentIds.contains(shipmentId)) {
            throw new IllegalArgumentException("Shipment " + shipmentId.getValue() + " is not assigned to this load.");
        }
        this.shipmentIds.remove(shipmentId);
        this.updatedAt = OffsetDateTime.now();
    }


        public void assignCarrier(CarrierName carrierName) {
        if (this.status != LoadStatus.PLANNED) {
            throw new IllegalStateException("Cannot assign a carrier to a load that is not in PLANNED state.");
        }
        this.carrierName = Objects.requireNonNull(carrierName, "Carrier name cannot be null");
        this.status = LoadStatus.TENDERED; // Transition to TENDERED state
        this.updatedAt = OffsetDateTime.now();
    }

    public void unassignCarrier() {
        if (this.status != LoadStatus.TENDERED) {
            throw new IllegalStateException("Cannot unassign carrier from a load that is not in TENDERED state.");
        }
        this.carrierName = null;
        this.status = LoadStatus.PLANNED;
        this.updatedAt = OffsetDateTime.now();
    }


        public void tenderLoad(OffsetDateTime expiresAt, String notes) {
        if (this.status != LoadStatus.TENDERED) {
            throw new IllegalStateException("Cannot tender a load that is not in TENDERED state.");
        }
        if (this.carrierName == null) {
            throw new IllegalStateException("Cannot tender a load without an assigned carrier.");
        }
        this.tender = new Tender(TenderStatus.PENDING, expiresAt, null, null, null, notes);
        this.updatedAt = OffsetDateTime.now();
    }


        public void cancelTender() {
        if (this.tender == null || this.tender.status() != TenderStatus.PENDING) {
            throw new IllegalStateException("Cannot cancel a tender that is not in PENDING state.");
        }
        this.tender = Tender.notTendered(); // Reset tender status
        this.status = LoadStatus.TENDERED; // Revert to TENDERED state
        this.updatedAt = OffsetDateTime.now();
    }

    public void recordTenderDecision(Tender.Decision decision, String respondedBy, String reason) {
        if (this.tender == null || this.tender.status() != TenderStatus.PENDING) {
            throw new IllegalStateException("Cannot record tender decision for a load that is not in PENDING state.");
        }
        this.tender = new Tender(TenderStatus.valueOf(decision.name()), this.tender.expiresAt(), OffsetDateTime.now(), respondedBy, decision, reason);
        if (decision == Tender.Decision.ACCEPTED) {
            this.status = LoadStatus.TENDER_ACCEPTED;
        } else {
            this.status = LoadStatus.PLANNED; // Or some other appropriate status
        }
        this.updatedAt = OffsetDateTime.now();
    }


        public void schedulePickup(String confirmationNumber, OffsetDateTime scheduledFor, Location location, String contactName, String contactPhone, String instructions) {
        if (this.status != LoadStatus.BOOKED) {
            throw new IllegalStateException("Cannot schedule pickup for a load that is not in BOOKED state.");
        }
        this.pickup = new Pickup(confirmationNumber, scheduledFor, location, contactName, contactPhone, instructions);
        this.updatedAt = OffsetDateTime.now();
    }

    public void cancelPickup() {
        if (this.pickup == null) {
            throw new IllegalStateException("No pickup scheduled to cancel.");
        }
        this.pickup = null;
        this.updatedAt = OffsetDateTime.now();
    }


    // Getters
    public LoadId getId() { return id; }
    public String getReference() { return reference; }
    public LoadStatus getStatus() { return status; }
    public CarrierName getCarrierName() { return carrierName; }
    public Set<ShipmentId> getShipmentIds() { return shipmentIds; }
    public Location getOrigin() { return origin; }
    public Location getDestination() { return destination; }
    public LocalDate getRequestedPickupDate() { return requestedPickupDate; }
    public LocalDate getRequestedDeliveryDate() { return requestedDeliveryDate; }
    public Pickup getPickup() { return pickup; }
    public Tender getTender() { return tender; }
    public String getNotes() { return notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
