package com.paklog.shipment.infrastructure.job;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.application.MetricsService;
import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.config.TrackingJobProperties;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.TrackingEvent;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.TrackingUpdate;
import com.paklog.shipment.domain.exception.CarrierException;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TrackingJobServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ShipmentApplicationService shipmentApplicationService;

    @Mock
    private ICarrierAdapter carrierAdapter;

    @Mock
    private MetricsService metricsService;

    private TrackingJobService trackingJobService;

    private Shipment inTransitShipment;
    private ObservationRegistry observationRegistry;

    @BeforeEach
    void setUp() {
        observationRegistry = ObservationRegistry.create();
        inTransitShipment = Shipment.create(com.paklog.shipment.domain.OrderId.of("order-1"), CarrierName.FEDEX,
                OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        inTransitShipment.dispatch(TrackingNumber.of("trk-1"), "label".getBytes(), OffsetDateTime.parse("2024-01-01T01:00:00Z"));

        when(carrierAdapter.getCarrierName()).thenReturn(CarrierName.FEDEX);
        TrackingJobProperties properties = new TrackingJobProperties();
        properties.setBatchSize(1);
        trackingJobService = new TrackingJobService(
                shipmentRepository,
                shipmentApplicationService,
                List.of(carrierAdapter),
                properties,
                observationRegistry,
                metricsService
        );
    }

    @Test
    void appliesTrackingUpdatesWhenCarrierReturnsEvents() {
        TrackingEvent newEvent = new TrackingEvent("IN_TRANSIT", "Departed", "NY",
                OffsetDateTime.parse("2024-01-02T00:00:00Z"), "CODE", "Details");
        TrackingUpdate update = new TrackingUpdate(newEvent, false, List.of(newEvent));

        when(shipmentRepository.findPageInTransit(null, 1)).thenReturn(List.of(inTransitShipment));
        when(shipmentRepository.findPageInTransit(inTransitShipment.getId().toString(), 1)).thenReturn(List.of());
        when(carrierAdapter.getTrackingStatus(TrackingNumber.of("trk-1"))).thenReturn(Optional.of(update));

        trackingJobService.updateTrackingStatus();

        verify(shipmentApplicationService).updateShipmentTracking(inTransitShipment.getId(), update);
    }

    @Test
    void skipsWhenNoNewEventsFromCarrier() {
        when(shipmentRepository.findPageInTransit(null, 1)).thenReturn(List.of(inTransitShipment));
        when(shipmentRepository.findPageInTransit(inTransitShipment.getId().toString(), 1)).thenReturn(List.of());
        when(carrierAdapter.getTrackingStatus(TrackingNumber.of("trk-1"))).thenReturn(Optional.of(new TrackingUpdate(
                new TrackingEvent("IN_TRANSIT", "No change", "NY",
                        OffsetDateTime.parse("2024-01-02T00:00:00Z"), "CODE", "Details"),
                false,
                List.of()
        )));

        trackingJobService.updateTrackingStatus();

        verify(shipmentApplicationService, never()).updateShipmentTracking(any(ShipmentId.class), any());
    }

    @Test
    void handlesCarrierExceptionGracefully() {
        when(shipmentRepository.findPageInTransit(null, 1)).thenReturn(List.of(inTransitShipment));
        when(shipmentRepository.findPageInTransit(inTransitShipment.getId().toString(), 1)).thenReturn(List.of());
        when(carrierAdapter.getTrackingStatus(TrackingNumber.of("trk-1")))
                .thenThrow(new CarrierException("fail", "FedEx"));

        trackingJobService.updateTrackingStatus();

        verify(shipmentApplicationService, never()).updateShipmentTracking(any(), any());
    }

    @Test
    void ignoresShipmentsWithoutAdapters() {
        Shipment unknownCarrierShipment = Shipment.create(com.paklog.shipment.domain.OrderId.of("order-2"), CarrierName.UPS,
                OffsetDateTime.parse("2024-01-01T00:00:00Z"));
        unknownCarrierShipment.dispatch(TrackingNumber.of("trk-2"), "label".getBytes(), OffsetDateTime.parse("2024-01-01T01:00:00Z"));

        when(shipmentRepository.findPageInTransit(null, 1)).thenReturn(List.of(unknownCarrierShipment));
        when(shipmentRepository.findPageInTransit(unknownCarrierShipment.getId().toString(), 1)).thenReturn(List.of());

        trackingJobService.updateTrackingStatus();

        verifyNoInteractions(shipmentApplicationService);
    }
}
