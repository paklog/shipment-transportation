package com.paklog.shipment.application;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.LoadStatus;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.domain.ShippingCost;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.repository.ILoadRepository;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void rateLoadUsesCarrierAdapter() {
        LoadId loadId = LoadId.generate();
        Load load = new Load(loadId);
        load.assignCarrier(CarrierName.FEDEX);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        ShippingCost expected = new ShippingCost(new BigDecimal("12.50"), "USD", 3);
        when(carrierAdapter.rateLoad(load)).thenReturn(expected);

        ShippingCost result = service.rateLoad(loadId);

        assertEquals(expected, result);
        verify(carrierAdapter).rateLoad(load);
    }

    @Test
    void rateLoadThrowsWhenCarrierMissing() {
        LoadId loadId = LoadId.generate();
        Load load = new Load(loadId);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.rateLoad(loadId));
        assertTrue(ex.getMessage().contains("must have a carrier"));
    }

    @Test
    void rateLoadThrowsWhenAdapterMissing() {
        LoadId loadId = LoadId.generate();
        Load load = new Load(loadId);
        load.assignCarrier(CarrierName.UPS);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.rateLoad(loadId));
        assertTrue(ex.getMessage().contains("No adapter"));
    }

    @Test
    void tenderLoadPersistsWhenAcceptedByCarrier() {
        LoadId loadId = LoadId.generate();
        Load load = new Load(loadId);
        load.assignCarrier(CarrierName.FEDEX);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        when(carrierAdapter.tenderLoad(load)).thenReturn(true);

        service.tenderLoad(loadId);

        assertEquals(LoadStatus.TENDERED, load.getStatus());
        verify(loadRepository).save(load);
        assertEquals(1.0, metricsService.loadsTendered.count());
    }

    @Test
    void tenderLoadSkipsWhenCarrierRejects() {
        LoadId loadId = LoadId.generate();
        Load load = new Load(loadId);
        load.assignCarrier(CarrierName.FEDEX);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        when(carrierAdapter.tenderLoad(load)).thenReturn(false);

        service.tenderLoad(loadId);

        assertEquals(LoadStatus.OPEN, load.getStatus());
        verify(loadRepository, never()).save(load);
        assertEquals(0.0, metricsService.loadsTendered.count());
    }

    @Test
    void tenderLoadThrowsWhenNoAdapter() {
        LoadId loadId = LoadId.generate();
        Load load = new Load(loadId);
        load.assignCarrier(CarrierName.UPS);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.tenderLoad(loadId));
        assertTrue(ex.getMessage().contains("No adapter"));
    }

    @Test
    void handleTenderResponseBooksWhenAccepted() {
        LoadId loadId = LoadId.generate();
        Load load = new Load(loadId);
        load.assignCarrier(CarrierName.FEDEX);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));

        service.handleTenderResponse(loadId, true);

        assertEquals(LoadStatus.BOOKED, load.getStatus());
        assertEquals(1.0, metricsService.loadsBooked.count());
        verify(loadRepository).save(load);
    }

    @Test
    void schedulePickupRequiresBookedLoad() {
        LoadId loadId = LoadId.generate();
        Load load = new Load(loadId);
        load.assignCarrier(CarrierName.FEDEX);
        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));

        assertThrows(IllegalStateException.class, () -> service.schedulePickup(loadId));
    }

    @Test
    void addShipmentToLoadPersistsAssignment() {
        LoadId loadId = LoadId.generate();
        ShipmentId shipmentId = ShipmentId.generate();
        Load load = new Load(loadId);
        Shipment shipment = Shipment.restore(
                shipmentId,
                OrderId.of("order-1"),
                CarrierName.FEDEX,
                TrackingNumber.of("trk-assign"),
                "label".getBytes(),
                ShipmentStatus.DISPATCHED,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T01:00:00Z"),
                null,
                List.of()
        );

        when(loadRepository.findById(loadId)).thenReturn(Optional.of(load));
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));

        service.addShipmentToLoad(loadId, shipmentId);

        assertTrue(load.getShipmentIds().contains(shipmentId));
        verify(loadRepository).save(load);
    }
}
