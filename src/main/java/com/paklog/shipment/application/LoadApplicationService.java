
package com.paklog.shipment.application;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.application.command.CreateLoadCommand;
import com.paklog.shipment.application.command.UpdateLoadCommand;
import com.paklog.shipment.application.exception.LoadNotFoundException;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.LoadStatus;
import com.paklog.shipment.domain.Location;
import com.paklog.shipment.domain.Pickup;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShippingCost;
import com.paklog.shipment.domain.Tender;
import com.paklog.shipment.domain.repository.ILoadRepository;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LoadApplicationService {

    private final ILoadRepository loadRepository;
    private final ShipmentRepository shipmentRepository;
    private final Map<CarrierName, ICarrierAdapter> carrierAdapters;
    private final MetricsService metricsService;

    public LoadApplicationService(ILoadRepository loadRepository, ShipmentRepository shipmentRepository, List<ICarrierAdapter> carrierAdapterList, MetricsService metricsService) {
        this.loadRepository = loadRepository;
        this.shipmentRepository = shipmentRepository;
        this.carrierAdapters = carrierAdapterList.stream()
                .collect(Collectors.toMap(
                        ICarrierAdapter::getCarrierName,
                        Function.identity()
                ));
        this.metricsService = metricsService;
    }

    @Transactional
    public Load createLoad(CreateLoadCommand command) {
        Set<ShipmentId> shipmentIds = command.shipments().stream()
                .map(ShipmentId::of)
                .collect(Collectors.toSet());

        Load newLoad = new Load(
                command.reference(),
                shipmentIds,
                command.origin(),
                command.destination(),
                command.requestedPickupDate(),
                command.requestedDeliveryDate(),
                command.notes()
        );

        loadRepository.save(newLoad);
        metricsService.loadsCreated.increment();
        return newLoad;
    }

    @Transactional(readOnly = true)
    public Load getLoad(LoadId loadId) {
        return loadRepository.findById(loadId)
                .orElseThrow(() -> new LoadNotFoundException("Load not found: " + loadId.getValue()));
    }

    @Transactional(readOnly = true)
    public Page<Load> getLoads(LoadStatus status, CarrierName carrierName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return loadRepository.findAll(pageable, status, carrierName);
    }

    @Transactional
    public Load updateLoad(LoadId loadId, UpdateLoadCommand command) {
        Load existingLoad = getLoad(loadId);

        existingLoad.applyUpdate(
                command.reference(),
                command.requestedPickupDate(),
                command.requestedDeliveryDate(),
                command.status(),
                command.notes()
        );

        loadRepository.save(existingLoad);
        return existingLoad;
    }

    @Transactional
    public void deleteLoad(LoadId loadId) {
        Load existingLoad = getLoad(loadId);
        // In a real application, we would check load status and other business rules before deleting.
        // For now, we simply delete.
        loadRepository.delete(existingLoad);
    }

    @Transactional(readOnly = true)
    public Page<Shipment> getShipmentsForLoad(LoadId loadId, int page, int size) {
        Load load = getLoad(loadId);
        List<Shipment> shipments = shipmentRepository.findAllById(load.getShipmentIds().stream().collect(Collectors.toList()));
        Pageable pageable = PageRequest.of(page, size);
        int start = Math.min((int) pageable.getOffset(), shipments.size());
        int end = Math.min(start + pageable.getPageSize(), shipments.size());
        List<Shipment> pageContent = shipments.subList(start, end);
        return new PageImpl<>(pageContent, pageable, shipments.size());
    }

    @Transactional
    public Load assignShipmentsToLoad(LoadId loadId, Set<UUID> shipmentIds) {
        Load existingLoad = getLoad(loadId);
        Set<ShipmentId> domainShipmentIds = shipmentIds.stream().map(ShipmentId::of).collect(Collectors.toSet());
        existingLoad.addShipments(domainShipmentIds);
        loadRepository.save(existingLoad);
        return existingLoad;
    }

    @Transactional
    public void removeShipmentFromLoad(LoadId loadId, ShipmentId shipmentId) {
        Load existingLoad = getLoad(loadId);
        existingLoad.removeShipment(shipmentId);
        loadRepository.save(existingLoad);
    }

    @Transactional
    public Load assignCarrier(LoadId loadId, CarrierName carrierName, String scac, String contactName, String contactPhone) {
        Load existingLoad = getLoad(loadId);
        existingLoad.assignCarrier(carrierName); // Assuming assignCarrier in domain.Load can take more details if needed
        loadRepository.save(existingLoad);
        return existingLoad;
    }

    @Transactional
    public void unassignCarrier(LoadId loadId) {
        Load existingLoad = getLoad(loadId);
        existingLoad.unassignCarrier();
        loadRepository.save(existingLoad);
    }

    @Transactional(readOnly = true)
    public ShippingCost rateLoad(LoadId loadId) {
        Load load = getLoad(loadId);
        if (load.getCarrierName() == null) {
            throw new IllegalStateException("Load must have a carrier assigned before it can be rated.");
        }
        ICarrierAdapter carrierAdapter = carrierAdapters.get(load.getCarrierName());
        if (carrierAdapter == null) {
            throw new IllegalStateException("No adapter found for carrier: " + load.getCarrierName());
        }
        return carrierAdapter.rateLoad(load);
    }

    @Transactional(readOnly = true)
    public Tender getTender(LoadId loadId) {
        Load load = getLoad(loadId);
        return load.getTender();
    }

    @Transactional
    public Load tenderLoad(LoadId loadId, OffsetDateTime expiresAt, String notes) {
        Load existingLoad = getLoad(loadId);
        existingLoad.tenderLoad(expiresAt, notes);
        loadRepository.save(existingLoad);
        return existingLoad;
    }

    @Transactional
    public void cancelTender(LoadId loadId) {
        Load existingLoad = getLoad(loadId);
        existingLoad.cancelTender();
        loadRepository.save(existingLoad);
    }

    @Transactional
    public Load recordTenderDecision(LoadId loadId, Tender.Decision decision, String respondedBy, String reason) {
        Load existingLoad = getLoad(loadId);
        existingLoad.recordTenderDecision(decision, respondedBy, reason); // Assuming this method exists in domain.Load
        loadRepository.save(existingLoad);
        return existingLoad;
    }

    @Transactional(readOnly = true)
    public Pickup getPickup(LoadId loadId) {
        Load load = getLoad(loadId);
        return load.getPickup();
    }

    @Transactional
    public Load schedulePickup(LoadId loadId, OffsetDateTime scheduledFor, Location location, String contactName, String contactPhone, String instructions) {
        Load existingLoad = getLoad(loadId);
        String confirmationNumber = UUID.randomUUID().toString(); // Generate confirmation number
        existingLoad.schedulePickup(confirmationNumber, scheduledFor, location, contactName, contactPhone, instructions);
        loadRepository.save(existingLoad);
        return existingLoad;
    }

    @Transactional
    public void cancelPickup(LoadId loadId) {
        Load existingLoad = getLoad(loadId);
        existingLoad.cancelPickup();
        loadRepository.save(existingLoad);
    }

    @Transactional(readOnly = true)
    public String getBillOfLading(LoadId loadId) {
        Load load = getLoad(loadId);
        // This is a placeholder. Real implementation would generate a BOL document.
        return "Bill of Lading for Load: " + load.getReference() + " (ID: " + load.getId().getValue() + ")";
    }
}
