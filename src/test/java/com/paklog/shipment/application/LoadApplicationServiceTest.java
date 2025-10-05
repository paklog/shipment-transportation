package com.paklog.shipment.application;

import com.paklog.shipment.TestFixtures;
import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.LoadStatus;
import com.paklog.shipment.domain.Location;
import com.paklog.shipment.domain.Pickup;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShippingCost;
import com.paklog.shipment.domain.Tender;
import com.paklog.shipment.domain.TenderStatus;
import com.paklog.shipment.domain.repository.ILoadRepository;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadApplicationServiceTest {

    @Mock
    private ILoadRepository loadRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ICarrierAdapter carrierAdapter;

    private MetricsService metricsService;
    private LoadApplicationService service;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService(new SimpleMeterRegistry());
        when(carrierAdapter.getCarrierName()).thenReturn(CarrierName.FEDEX);
        service = new LoadApplicationService(loadRepository, shipmentRepository, List.of(carrierAdapter), metricsService);
    }

    @Test
    void rateLoadDelegatesToCarrierAdapter() {
        Load load = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        load.assignCarrier(CarrierName.FEDEX);
        when(loadRepository.findById(load.getId())).thenReturn(Optional.of(load));
        ShippingCost expected = new ShippingCost(new BigDecimal("125.50"), "USD", 3);
        when(carrierAdapter.rateLoad(load)).thenReturn(expected);

        ShippingCost result = service.rateLoad(load.getId());

        assertEquals(expected, result);
        verify(carrierAdapter).rateLoad(load);
    }

    @Test
    void assignShipmentsToLoadAddsIdentifiers() {
        Load load = TestFixtures.sampleLoad(LoadStatus.PLANNED, CarrierName.FEDEX);
        when(loadRepository.findById(load.getId())).thenReturn(Optional.of(load));

        ShipmentId newShipment = ShipmentId.generate();
        service.assignShipmentsToLoad(load.getId(), Set.of(newShipment.getValue()));

        boolean present = load.getShipmentIds().stream()
                .anyMatch(id -> id.getValue().equals(newShipment.getValue()));
        assertTrue(present);
        verify(loadRepository).save(load);
    }

    @Test
    void tenderLoadCreatesPendingTender() {
        Load load = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        load.assignCarrier(CarrierName.FEDEX);
        when(loadRepository.findById(load.getId())).thenReturn(Optional.of(load));

        OffsetDateTime expiresAt = OffsetDateTime.parse("2024-05-10T17:00:00Z");
        Load updated = service.tenderLoad(load.getId(), expiresAt, "call carrier");

        assertEquals(TenderStatus.PENDING, updated.getTender().status());
        assertEquals(expiresAt, updated.getTender().expiresAt());
        verify(loadRepository).save(load);
    }

    @Test
    void recordTenderDecisionUpdatesStatus() {
        Load load = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        load.assignCarrier(CarrierName.FEDEX);
        when(loadRepository.findById(load.getId())).thenReturn(Optional.of(load));

        service.tenderLoad(load.getId(), OffsetDateTime.parse("2024-05-10T17:00:00Z"), null);
        Load updated = service.recordTenderDecision(load.getId(), Tender.Decision.ACCEPTED, "ops@carrier.com", "confirmed");

        assertEquals(LoadStatus.TENDER_ACCEPTED, updated.getStatus());
        assertEquals(TenderStatus.ACCEPTED, updated.getTender().status());
        verify(loadRepository, times(2)).save(load);
    }

    @Test
    void schedulePickupSetsPickupDetails() {
        Load load = TestFixtures.sampleLoad(LoadStatus.BOOKED, CarrierName.FEDEX);
        when(loadRepository.findById(load.getId())).thenReturn(Optional.of(load));

        Location location = TestFixtures.sampleLocation();
        OffsetDateTime scheduledFor = OffsetDateTime.parse("2024-05-15T14:00:00Z");

        Load result = service.schedulePickup(load.getId(), scheduledFor, location, "Dock", "123", "Instructions");

        Pickup pickup = result.getPickup();
        assertNotNull(pickup);
        assertEquals(scheduledFor, pickup.scheduledFor());
        assertEquals(location, pickup.location());
        verify(loadRepository).save(load);
    }
}
