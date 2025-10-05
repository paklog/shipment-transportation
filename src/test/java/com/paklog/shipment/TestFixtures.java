package com.paklog.shipment;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.LoadStatus;
import com.paklog.shipment.domain.Location;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.domain.Tender;
import com.paklog.shipment.domain.TenderStatus;
import com.paklog.shipment.domain.TrackingEvent;
import com.paklog.shipment.domain.TrackingNumber;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static OffsetDateTime odt(String value) {
        return OffsetDateTime.parse(value);
    }

    public static Location sampleLocation() {
        return new Location(
                "Warehouse",
                "123 Main St",
                null,
                "Portland",
                "OR",
                "97201",
                "US"
        );
    }

    public static Load sampleLoad(LoadStatus status, CarrierName carrierName) {
        Set<ShipmentId> shipmentIds = new HashSet<>(Set.of(ShipmentId.generate()));
        Tender tender = Tender.notTendered();
        if (status == LoadStatus.TENDERED) {
            tender = new Tender(TenderStatus.PENDING, odt("2024-05-10T17:00:00Z"), null, null, null, null);
        } else if (status == LoadStatus.TENDER_ACCEPTED) {
            tender = new Tender(TenderStatus.ACCEPTED, odt("2024-05-10T17:00:00Z"), odt("2024-05-09T09:45:00Z"), "dispatch@carrier.com", Tender.Decision.ACCEPTED, null);
        }

        return Load.restore(
                LoadId.generate(),
                "REF-123",
                status,
                carrierName,
                shipmentIds,
                sampleLocation(),
                new Location("Store", "1 Market St", null, "Denver", "CO", "80202", "US"),
                LocalDate.parse("2024-05-10"),
                LocalDate.parse("2024-05-12"),
                null,
                tender,
                "Handle with care",
                odt("2024-05-01T08:00:00Z"),
                odt("2024-05-01T08:00:00Z")
        );
    }

    public static Shipment sampleShipment(ShipmentStatus status, CarrierName carrierName) {
        OffsetDateTime createdAt = odt("2024-05-01T08:00:00Z");
        OffsetDateTime dispatchedAt = status != ShipmentStatus.CREATED ? createdAt.plusHours(2) : null;
        OffsetDateTime deliveredAt = status == ShipmentStatus.DELIVERED ? createdAt.plusDays(2) : null;
        List<TrackingEvent> events = status == ShipmentStatus.CREATED ? List.of() : List.of(
                new TrackingEvent("IN_TRANSIT", "Departed origin", "Portland, OR", createdAt.plusHours(3), "DEPARTED", ""));

        return Shipment.restore(
                ShipmentId.generate(),
                OrderId.of("ORD-123"),
                carrierName,
                TrackingNumber.of("TRK-123"),
                "label".getBytes(),
                status,
                createdAt,
                dispatchedAt,
                deliveredAt,
                events,
                null,
                createdAt.plusDays(1)
        );
    }
}
