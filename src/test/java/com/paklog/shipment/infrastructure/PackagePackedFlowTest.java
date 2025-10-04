package com.paklog.shipment.infrastructure;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.application.MetricsService;
import com.paklog.shipment.application.PackageRetrievalService;
import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.config.OutboxProperties;
import com.paklog.shipment.config.ShipmentEventProperties;
import com.paklog.shipment.domain.CarrierInfo;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.Package;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.repository.ILoadRepository;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import com.paklog.shipment.domain.services.CarrierSelectionService;
import com.paklog.shipment.domain.events.PackagePackedCloudEvent;
import com.paklog.shipment.infrastructure.OutboxEvent;
import com.paklog.shipment.infrastructure.OutboxEventRepository;
import com.paklog.shipment.infrastructure.OutboxService;
import com.paklog.shipment.infrastructure.events.ShipmentEventPublisherImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PackagePackedFlowTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private ILoadRepository loadRepository;

    @Mock
    private CarrierSelectionService carrierSelectionService;

    @Mock
    private PackageRetrievalService packageRetrievalService;

    @Mock
    private ICarrierAdapter carrierAdapter;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private LoadApplicationService loadApplicationService;

    private MetricsService metricsService;
    private ShipmentApplicationService shipmentApplicationService;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService(new SimpleMeterRegistry());
        when(carrierAdapter.getCarrierName()).thenReturn(CarrierName.FEDEX);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shipmentRepository.findByOrderId(any())).thenReturn(Optional.empty());
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxProperties outboxProperties = new OutboxProperties();
        OutboxService outboxService = new OutboxService(outboxEventRepository, outboxProperties);
        ShipmentEventProperties shipmentEventProperties = new ShipmentEventProperties();
        shipmentEventProperties.getDispatched().setType("com.paklog.shipment.dispatched.v1");
        shipmentEventProperties.getDispatched().setTopic("fulfillment.shipment.v1.events");
        shipmentEventProperties.getDelivered().setType("com.paklog.shipment.delivered.v1");
        shipmentEventProperties.getDelivered().setTopic("fulfillment.shipment.v1.events");
        ShipmentEventPublisherImpl eventPublisher = new ShipmentEventPublisherImpl(outboxService, new com.fasterxml.jackson.databind.ObjectMapper(), shipmentEventProperties);
        shipmentApplicationService = new ShipmentApplicationService(
                shipmentRepository,
                loadApplicationService,
                metricsService,
                packageRetrievalService,
                carrierSelectionService,
                List.of(carrierAdapter),
                eventPublisher
        );
    }

    @Test
    void endToEndPackagePackedCreatesShipmentAndOutboxEvent() {
        Package packageDetails = new Package("pkg-flow", 2.0, 10.0, 5.0, 5.0, "BOX");
        when(packageRetrievalService.getPackageDetails("pkg-flow")).thenReturn(packageDetails);
        when(carrierSelectionService.selectBestCarrier(packageDetails)).thenReturn(CarrierName.FEDEX);
        when(carrierAdapter.createShipment(packageDetails, OrderId.of("order-flow"), "pkg-flow"))
                .thenReturn(new CarrierInfo("trk-flow", "label".getBytes(), CarrierName.FEDEX));

        PackagePackedEventConsumer consumer = new PackagePackedEventConsumer(shipmentApplicationService, metricsService, ObservationRegistry.create());
        consumer.packagePacked().accept(new PackagePackedCloudEvent("pkg-flow", "order-flow", Instant.parse("2024-01-01T10:00:00Z")));

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent outboxEvent = outboxCaptor.getValue();

        assertThat(outboxEvent.getAggregateType()).isEqualTo("Shipment");
        assertThat(outboxEvent.getDestination()).isEqualTo("fulfillment.shipment.v1.events");
        assertThat(metricsService.shipmentsCreated.count()).isEqualTo(1);
        verify(loadApplicationService).addShipmentToLoad(eq(LoadId.of("00000000-0000-0000-0000-000000000000")), any(ShipmentId.class));
    }
}
