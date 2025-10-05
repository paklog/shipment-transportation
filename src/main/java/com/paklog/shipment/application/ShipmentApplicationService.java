package com.paklog.shipment.application;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.application.command.CreateShipmentCommand;
import com.paklog.shipment.application.exception.ShipmentCreationException;
import com.paklog.shipment.application.exception.ShipmentNotFoundException;
import com.paklog.shipment.application.port.ShipmentEventPublisher;
import com.paklog.shipment.domain.CarrierInfo;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.LoadId;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ShipmentApplicationService {

    private final ShipmentRepository shipmentRepository;
    private final LoadApplicationService loadApplicationService;
    private final MetricsService metricsService;
    private final PackageRetrievalService packageRetrievalService;
    private final CarrierSelectionService carrierSelectionService;
    private final Map<CarrierName, ICarrierAdapter> carrierAdapters;
    private final ShipmentEventPublisher eventPublisher;

    private static final LoadId UNASSIGNED_LOAD_ID = LoadId.of("00000000-0000-0000-0000-000000000000");

    public ShipmentApplicationService(ShipmentRepository shipmentRepository,
                                      LoadApplicationService loadApplicationService,
                                      MetricsService metricsService,
                                      PackageRetrievalService packageRetrievalService,
                                      CarrierSelectionService carrierSelectionService,
                                      List<ICarrierAdapter> carrierAdapterList,
                                      ShipmentEventPublisher eventPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.loadApplicationService = loadApplicationService;
        this.metricsService = metricsService;
        this.packageRetrievalService = packageRetrievalService;
        this.carrierSelectionService = carrierSelectionService;
        this.carrierAdapters = carrierAdapterList.stream()
                .collect(Collectors.toMap(ICarrierAdapter::getCarrierName, Function.identity()));
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Shipment createShipment(CreateShipmentCommand command) {
        Optional<Shipment> existingShipment = shipmentRepository.findByOrderId(command.getOrderId());
        if (existingShipment.isPresent()) {
            return existingShipment.get();
        }

        Package packageDetails = packageRetrievalService.getPackageDetails(command.getPackageId());
        CarrierName carrier = carrierSelectionService.selectBestCarrier(packageDetails);
        ICarrierAdapter carrierAdapter = resolveCarrierAdapter(carrier);

        try {
            CarrierInfo carrierInfo = carrierAdapter.createShipment(packageDetails, command.getOrderId(), command.getPackageId());
            TrackingNumber trackingNumber = TrackingNumber.of(carrierInfo.getTrackingNumber());
            OffsetDateTime now = OffsetDateTime.now();

            Shipment shipment = Shipment.create(command.getOrderId(), carrier, now);
            shipment.dispatch(trackingNumber, carrierInfo.getLabelData(), now);

            Shipment persisted = shipmentRepository.save(shipment);
            metricsService.shipmentsCreated.increment();
//            loadApplicationService.addShipmentToLoad(UNASSIGNED_LOAD_ID, persisted.getId());
            eventPublisher.shipmentDispatched(persisted);
            return persisted;
        } catch (CarrierException ex) {
            throw new ShipmentCreationException("Carrier " + carrier + " failed to create shipment", ex);
        }
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentTracking(ShipmentId shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentId));
    }

    @Transactional
    public void updateShipmentTracking(ShipmentId shipmentId, TrackingUpdate trackingUpdate) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalStateException("Shipment not found: " + shipmentId));

        trackingUpdate.getNewEvents().forEach(shipment::addTrackingEvent);

        if (trackingUpdate.isDelivered() && !shipment.isDelivered()) {
            TrackingEvent latestEvent = trackingUpdate.getLatestEvent();
            boolean alreadyAdded = trackingUpdate.getNewEvents().stream()
                    .anyMatch(event -> event.equals(latestEvent));
            TrackingEvent deliveryEvent = alreadyAdded ? null : latestEvent;
            shipment.markAsDelivered(deliveryEvent, latestEvent.getTimestamp());
            eventPublisher.shipmentDelivered(shipment);
        }

        shipmentRepository.save(shipment);
    }

    @Transactional(readOnly = true)
    public Page<Shipment> getShipments(com.paklog.shipment.domain.ShipmentStatus status, CarrierName carrierName, int page, int size) {
        List<Shipment> shipments = shipmentRepository.findAll();

        var filtered = shipments.stream()
                .filter(shipment -> status == null || shipment.getStatus() == status)
                .filter(shipment -> carrierName == null || carrierName.equals(shipment.getCarrierName()))
                .sorted(Comparator.comparing(Shipment::getCreatedAt).reversed())
                .toList();

        int fromIndex = Math.min(page * size, filtered.size());
        int toIndex = Math.min(fromIndex + size, filtered.size());
        List<Shipment> pageContent = filtered.subList(fromIndex, toIndex);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public Shipment getShipment(ShipmentId shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentId.getValue()));
    }

    private ICarrierAdapter resolveCarrierAdapter(CarrierName carrier) {
        ICarrierAdapter adapter = carrierAdapters.get(carrier);
        if (adapter == null) {
            throw new ShipmentCreationException("No carrier adapter registered for " + carrier);
        }
        return adapter;
    }
}
