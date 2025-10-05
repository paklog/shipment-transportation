package com.paklog.shipment.adapter.fedex;

import com.paklog.shipment.TestFixtures;
import com.paklog.shipment.application.MetricsService;
import com.paklog.shipment.domain.CarrierInfo;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadStatus;
import com.paklog.shipment.domain.Package;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.TrackingUpdate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        Package pkg = new Package("pkg-1", 2.0, 5.0, 5.0, 5.0, "BOX");

        CarrierInfo carrierInfo = fedExAdapter.createShipment(pkg, OrderId.of("ord-1"), pkg.getPackageId());

        assertNotNull(carrierInfo);
        assertTrue(carrierInfo.getTrackingNumber().startsWith("dummy-tracking-number"));
        assertNotNull(carrierInfo.getLabelData());
        assertTrue(carrierInfo.getLabelData().length > 0);
        assertEquals(fedExAdapter.getCarrierName(), carrierInfo.getCarrierName());
    }

    @Test
    void getTrackingStatusReturnsEmptyByDefault() {
        Optional<TrackingUpdate> update = fedExAdapter.getTrackingStatus(TrackingNumber.of("trk-123"));

        assertTrue(update.isEmpty());
    }

    @Test
    void rateLoadCalculatesCost() {
        Load load = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        load.assignCarrier(fedExAdapter.getCarrierName());

        assertNotNull(fedExAdapter.rateLoad(load));
    }
}
