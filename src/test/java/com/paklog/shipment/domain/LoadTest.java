package com.paklog.shipment.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class LoadTest {

    private static final Location ORIGIN = new Location("Origin", "123", null, "Portland", "OR", "97201", "US");
    private static final Location DEST = new Location("Dest", "456", null, "Denver", "CO", "80202", "US");
    private static final LocalDate PICKUP = LocalDate.parse("2024-05-10");
    private static final LocalDate DELIVERY = LocalDate.parse("2024-05-12");

    @Test
    void assignCarrierTransitionsToTendered() {
        Load load = newLoad();
        load.assignCarrier(CarrierName.FEDEX);

        assertEquals(LoadStatus.TENDERED, load.getStatus());
        assertEquals(CarrierName.FEDEX, load.getCarrierName());
    }

    @Test
    void tenderLoadRequiresAssignedCarrier() {
        Load load = newLoad();
        assertThrows(IllegalStateException.class, () -> load.tenderLoad(OffsetDateTime.now(), null));
    }

    @Test
    void tenderAndCancelResetsTenderState() {
        Load load = newLoad();
        load.assignCarrier(CarrierName.FEDEX);
        load.tenderLoad(OffsetDateTime.parse("2024-05-10T17:00:00Z"), "notes");

        assertEquals(TenderStatus.PENDING, load.getTender().status());

        load.cancelTender();
        assertEquals(LoadStatus.TENDERED, load.getStatus());
        assertEquals(TenderStatus.NOT_TENDERED, load.getTender().status());
    }

    @Test
    void recordTenderDecisionAcceptedBooksLoad() {
        Load load = tenderedLoad();
        load.recordTenderDecision(Tender.Decision.ACCEPTED, "ops", "ok");

        assertEquals(LoadStatus.TENDER_ACCEPTED, load.getStatus());
        assertEquals(TenderStatus.ACCEPTED, load.getTender().status());
        assertEquals("ops", load.getTender().respondedBy());
    }

    @Test
    void recordTenderDecisionDeclinedReturnsToPlanned() {
        Load load = tenderedLoad();
        load.recordTenderDecision(Tender.Decision.DECLINED, "ops", "capacity");

        assertEquals(LoadStatus.PLANNED, load.getStatus());
        assertEquals(TenderStatus.DECLINED, load.getTender().status());
    }

    @Test
    void scheduleAndCancelPickupUpdatesState() {
        Load load = bookedLoad();
        OffsetDateTime scheduledFor = OffsetDateTime.parse("2024-05-15T09:00:00Z");
        load.schedulePickup("CONF-1", scheduledFor, ORIGIN, "Dock", "555", "instructions");

        assertNotNull(load.getPickup());
        assertEquals(scheduledFor, load.getPickup().scheduledFor());

        load.cancelPickup();
        assertNull(load.getPickup());
    }

    @Test
    void addShipmentPreventsDuplicates() {
        Load load = newLoad();
        ShipmentId shipment = load.getShipmentIds().iterator().next();
        assertThrows(IllegalArgumentException.class, () -> load.addShipments(Set.of(shipment)));
    }

    @Test
    void removeShipmentUpdatesSet() {
        Load load = newLoad();
        ShipmentId shipment = load.getShipmentIds().iterator().next();
        load.removeShipment(shipment);
        assertFalse(load.getShipmentIds().contains(shipment));
    }

    private Load newLoad() {
        return new Load(
                "REF-123",
                Set.of(ShipmentId.generate()),
                ORIGIN,
                DEST,
                PICKUP,
                DELIVERY,
                "notes"
        );
    }

    private Load tenderedLoad() {
        Load load = newLoad();
        load.assignCarrier(CarrierName.FEDEX);
        load.tenderLoad(OffsetDateTime.parse("2024-05-10T17:00:00Z"), "notes");
        return load;
    }

    private Load bookedLoad() {
        Load load = tenderedLoad();
        load.recordTenderDecision(Tender.Decision.ACCEPTED, "ops", "ok");
        return load;
    }
}
