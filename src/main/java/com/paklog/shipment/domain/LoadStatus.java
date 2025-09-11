package com.paklog.shipment.domain;

public enum LoadStatus {
    OPEN,      // The load is being planned and shipments can be added.
    TENDERED,  // The load has been offered to a carrier.
    BOOKED,    // A carrier has been assigned and confirmed.
    SHIPPED,   // The load has departed from the warehouse.
    IN_TRANSIT,// The load is on its way to the destination.
    COMPLETED, // The load has been delivered.
    CANCELLED; // The load has been cancelled.
}