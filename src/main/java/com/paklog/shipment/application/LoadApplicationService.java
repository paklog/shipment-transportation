package com.paklog.shipment.application;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.domain.*;
import com.paklog.shipment.domain.repository.ILoadRepository;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LoadApplicationService {

    private final ILoadRepository loadRepository;
    private final ShipmentRepository shipmentRepository;
    private final Map<String, ICarrierAdapter> carrierAdapters;
    private final MetricsService metricsService;

    public LoadApplicationService(ILoadRepository loadRepository, ShipmentRepository shipmentRepository, List<ICarrierAdapter> carrierAdapterList, MetricsService metricsService) {
        this.loadRepository = loadRepository;
        this.shipmentRepository = shipmentRepository;
        this.carrierAdapters = carrierAdapterList.stream()
                .collect(Collectors.toMap(
                        adapter -> adapter.getCarrierName().name(),
                        Function.identity()
                ));
        this.metricsService = metricsService;
    }

    @Transactional
    public LoadId createLoad() {
        LoadId newLoadId = LoadId.generate();
        Load newLoad = new Load(newLoadId);
        loadRepository.save(newLoad);
        metricsService.loadsCreated.increment(); // Increment metric
        return newLoadId;
    }

    @Transactional
    public void addShipmentToLoad(LoadId loadId, ShipmentId shipmentId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new IllegalStateException("Load not found: " + loadId));
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalStateException("Shipment not found: " + shipmentId));

        load.addShipment(shipment);
        loadRepository.save(load);
    }

    @Transactional(readOnly = true)
    public ShippingCost rateLoad(LoadId loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new IllegalStateException("Load not found: " + loadId));

        if (load.getCarrierName() == null) {
            throw new IllegalStateException("Load must have a carrier assigned before it can be rated.");
        }

        ICarrierAdapter carrierAdapter = carrierAdapters.get(load.getCarrierName().name());
        if (carrierAdapter == null) {
            throw new IllegalStateException("No adapter found for carrier: " + load.getCarrierName());
        }

        return carrierAdapter.rateLoad(load);
    }

    @Transactional
    public void tenderLoad(LoadId loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new IllegalStateException("Load not found: " + loadId));

        if (load.getCarrierName() == null) {
            throw new IllegalStateException("Load must have a carrier assigned before it can be tendered.");
        }

        ICarrierAdapter carrierAdapter = carrierAdapters.get(load.getCarrierName().name());
        if (carrierAdapter == null) {
            throw new IllegalStateException("No adapter found for carrier: " + load.getCarrierName());
        }

        boolean tendered = carrierAdapter.tenderLoad(load);

        if (tendered) {
            load.tender();
            loadRepository.save(load);
            metricsService.loadsTendered.increment(); // Increment metric
        }
    }

    @Transactional
    public void handleTenderResponse(LoadId loadId, boolean accepted) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new IllegalStateException("Load not found: " + loadId));

        if (accepted) {
            load.book();
            metricsService.loadsBooked.increment(); // Increment metric
        } else {
            load.reopen();
        }
        loadRepository.save(load);
    }

    @Transactional
    public String schedulePickup(LoadId loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new IllegalStateException("Load not found: " + loadId));

        if (load.getStatus() != LoadStatus.BOOKED) {
            throw new IllegalStateException("Can only schedule pickup for a BOOKED load.");
        }

        ICarrierAdapter carrierAdapter = carrierAdapters.get(load.getCarrierName().name());
        String confirmation = carrierAdapter.schedulePickup(load);

        // In a real system, you would save the confirmation to the Load aggregate
        return confirmation;
    }

    @Transactional
    public void assignCarrierToLoad(LoadId loadId, CarrierName carrierName) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new IllegalStateException("Load not found: " + loadId));
        load.assignCarrier(carrierName);
        loadRepository.save(load);
    }

    @Transactional
    public void shipConfirm(LoadId loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new IllegalStateException("Load not found: " + loadId));

        load.ship();
        loadRepository.save(load);
    }
}
