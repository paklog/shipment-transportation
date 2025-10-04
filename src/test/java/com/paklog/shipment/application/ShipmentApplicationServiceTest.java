package com.paklog.shipment.application;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.application.command.CreateShipmentCommand;
import com.paklog.shipment.application.exception.ShipmentCreationException;
import com.paklog.shipment.application.exception.ShipmentNotFoundException;
import com.paklog.shipment.application.port.ShipmentEventPublisher;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Package;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.domain.TrackingEvent;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.TrackingUpdate;
import com.paklog.shipment.domain.exception.CarrierException;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import com.paklog.shipment.domain.services.CarrierSelectionService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentApplicationServiceTest {

    private static final String PACKAGE_ID = "pkg-123";
    private static final String ORDER_ID = "ord-456";
    private static final LoadId UNASSIGNED_LOAD_ID = LoadId.of("00000000-0000-0000-0000-000000000000");

    @Mock
    private ShipmentRepository shipmentRepository;
    @Mock
    private LoadApplicationService loadApplicationService;
    private MetricsService metricsService;
    @Mock
    private PackageRetrievalService packageRetrievalService;
    @Mock
    private CarrierSelectionService carrierSelectionService;
    @Mock
    private ShipmentEventPublisher shipmentEventPublisher;
    @Mock
    private ICarrierAdapter carrierAdapter;

    private ShipmentApplicationService shipmentService;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService(new SimpleMeterRegistry());
        when(carrierAdapter.getCarrierName()).thenReturn(CarrierName.FEDEX);
        lenient().when(shipmentRepository.findByOrderId(any())).thenReturn(Optional.empty());

        shipmentService = new ShipmentApplicationService(
                shipmentRepository,
                loadApplicationService,
                metricsService,
                packageRetrievalService,
                carrierSelectionService,
                List.of(carrierAdapter),
                shipmentEventPublisher
        );
    }

    @Test
    void createShipmentDispatchesAndPersists() {
        Package packageDetails = new Package(5.0, 10.0, 10.0, 10.0, "BOX");
        when(packageRetrievalService.getPackageDetails(PACKAGE_ID)).thenReturn(packageDetails);
        when(carrierSelectionService.selectBestCarrier(packageDetails)).thenReturn(CarrierName.FEDEX);
        when(carrierAdapter.createShipment(packageDetails)).thenReturn("trk-123");
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Shipment result = shipmentService.createShipment(new CreateShipmentCommand(PACKAGE_ID, ORDER_ID));

        assertNotNull(result.getId());
        assertEquals("trk-123", result.getTrackingNumber().getValue());
        assertEquals(1.0, metricsService.shipmentsCreated.count());
        verify(loadApplicationService).addShipmentToLoad(eq(UNASSIGNED_LOAD_ID), eq(result.getId()));
        verify(shipmentEventPublisher).shipmentDispatched(result);
        verify(carrierAdapter).createShipment(packageDetails);
        verify(shipmentRepository).save(result);
    }

    @Test
    void createShipmentReturnsExistingWhenOrderAlreadyHandled() {
        Shipment existing = Shipment.restore(
                ShipmentId.generate(),
                OrderId.of(ORDER_ID),
                CarrierName.FEDEX,
                TrackingNumber.of("trk-existing"),
                ShipmentStatus.DISPATCHED,
                Instant.now(),
                Instant.now(),
                null,
                List.of()
        );
        when(shipmentRepository.findByOrderId(OrderId.of(ORDER_ID))).thenReturn(Optional.of(existing));

        Shipment result = shipmentService.createShipment(new CreateShipmentCommand(PACKAGE_ID, ORDER_ID));

        assertEquals(existing, result);
        assertEquals(0.0, metricsService.shipmentsCreated.count());
        verify(shipmentRepository, never()).save(any());
        verify(loadApplicationService, never()).addShipmentToLoad(any(), any());
        verify(shipmentEventPublisher, never()).shipmentDispatched(any());
    }

    @Test
    void createShipmentThrowsWhenAdapterMissing() {
        Package packageDetails = new Package(5.0, 10.0, 10.0, 10.0, "BOX");
        when(packageRetrievalService.getPackageDetails(PACKAGE_ID)).thenReturn(packageDetails);
        when(carrierSelectionService.selectBestCarrier(packageDetails)).thenReturn(CarrierName.UPS);

        assertThrows(ShipmentCreationException.class,
                () -> shipmentService.createShipment(new CreateShipmentCommand(PACKAGE_ID, ORDER_ID)));
    }

    @Test
    void createShipmentWrapsCarrierExceptions() {
        Package packageDetails = new Package(5.0, 10.0, 10.0, 10.0, "BOX");
        when(packageRetrievalService.getPackageDetails(PACKAGE_ID)).thenReturn(packageDetails);
        when(carrierSelectionService.selectBestCarrier(packageDetails)).thenReturn(CarrierName.FEDEX);
        when(carrierAdapter.createShipment(packageDetails)).thenThrow(new CarrierException("Carrier down", "FEDEX"));

        assertThrows(ShipmentCreationException.class,
                () -> shipmentService.createShipment(new CreateShipmentCommand(PACKAGE_ID, ORDER_ID)));
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void getShipmentTrackingReturnsShipment() {
        TrackingNumber trackingNumber = TrackingNumber.of("trk-123");
        Shipment shipment = Shipment.restore(
                ShipmentId.generate(),
                OrderId.of(ORDER_ID),
                CarrierName.FEDEX,
                trackingNumber,
                ShipmentStatus.DISPATCHED,
                Instant.now(),
                Instant.now(),
                null,
                List.of()
        );
        when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.of(shipment));

        Shipment result = shipmentService.getShipmentTracking(trackingNumber.getValue());

        assertEquals(shipment, result);
    }

    @Test
    void getShipmentTrackingThrowsWhenMissing() {
        TrackingNumber trackingNumber = TrackingNumber.of("trk-123");
        when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.empty());

        assertThrows(ShipmentNotFoundException.class,
                () -> shipmentService.getShipmentTracking(trackingNumber.getValue()));
    }

    @Test
    void updateShipmentTrackingPersistsNewEvents() {
        Shipment shipment = createDispatchedShipment();
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        TrackingEvent newEvent = new TrackingEvent("IN_TRANSIT", "Departed facility", "NY",
                Instant.parse("2024-01-02T00:00:00Z"), "CODE", "Details");
        TrackingUpdate update = new TrackingUpdate(newEvent, false, List.of(newEvent));

        shipmentService.updateShipmentTracking(shipment.getId(), update);

        assertEquals(List.of(newEvent), shipment.getTrackingEvents());
        verify(shipmentRepository).save(shipment);
        verify(shipmentEventPublisher, never()).shipmentDelivered(any());
    }

    @Test
    void updateShipmentTrackingMarksDelivered() {
        Shipment shipment = createDispatchedShipment();
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));

        TrackingEvent deliveredEvent = new TrackingEvent("DELIVERED", "Package delivered", "LA",
                Instant.parse("2024-01-02T10:00:00Z"), "DEL", "Left at door");
        TrackingUpdate update = new TrackingUpdate(deliveredEvent, true, List.of(deliveredEvent));

        shipmentService.updateShipmentTracking(shipment.getId(), update);

        assertEquals(ShipmentStatus.DELIVERED, shipment.getStatus());
        assertEquals(deliveredEvent.getTimestamp(), shipment.getDeliveredAt());
        verify(shipmentEventPublisher).shipmentDelivered(shipment);
        verify(shipmentRepository).save(shipment);
    }

    private Shipment createDispatchedShipment() {
        Shipment shipment = Shipment.create(OrderId.of(ORDER_ID), CarrierName.FEDEX,
                Instant.parse("2024-01-01T00:00:00Z"));
        shipment.dispatch(TrackingNumber.of("trk-123"), Instant.parse("2024-01-01T01:00:00Z"));
        return shipment;
    }
}
