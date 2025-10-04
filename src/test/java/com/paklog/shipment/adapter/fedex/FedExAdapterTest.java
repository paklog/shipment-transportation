package com.paklog.shipment.adapter.fedex;

import com.paklog.shipment.application.MetricsService;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.Package;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.TrackingUpdate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FedExAdapterTest {

    private FedExAdapter fedExAdapter;

    @BeforeEach
    void setUp() {
        fedExAdapter = new FedExAdapter(new FedExApiClient(), new MetricsService(new SimpleMeterRegistry()));
    }

    @Test
    void createShipmentReturnsTrackingNumber() {
        Package pkg = new Package(2.0, 5.0, 5.0, 5.0, "BOX");

        String tracking = fedExAdapter.createShipment(pkg);

        assertNotNull(tracking);
        assertTrue(tracking.startsWith("trk-"));
    }

    @Test
    void getTrackingStatusReturnsEmptyByDefault() {
        Optional<TrackingUpdate> update = fedExAdapter.getTrackingStatus(TrackingNumber.of("trk-123"));

        assertTrue(update.isEmpty());
    }

    @Test
    void rateLoadCalculatesCost() {
        Load load = new Load(LoadId.generate());
        load.assignCarrier(fedExAdapter.getCarrierName());
        load.addShipment(Shipment.restore(
                ShipmentId.generate(),
                OrderId.of("order"),
                fedExAdapter.getCarrierName(),
                TrackingNumber.of("trk-123"),
                ShipmentStatus.DISPATCHED,
                Instant.now(),
                Instant.now(),
                null,
                List.of()
        ));

        assertNotNull(fedExAdapter.rateLoad(load));
    }
}
