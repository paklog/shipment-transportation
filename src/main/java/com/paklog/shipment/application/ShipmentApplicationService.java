package com.paklog.shipment.application;

import com.paklog.shipment.domain.*;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShipmentApplicationService {

    private final ShipmentRepository shipmentRepository;
    private final LoadApplicationService loadApplicationService;

    private static final LoadId UNASSIGNED_LOAD_ID = LoadId.of("00000000-0000-0000-0000-000000000000");

    public ShipmentApplicationService(ShipmentRepository shipmentRepository, LoadApplicationService loadApplicationService) {
        this.shipmentRepository = shipmentRepository;
        this.loadApplicationService = loadApplicationService;
    }

    public void createShipment(OrderId orderId) {
        Shipment newShipment = new Shipment(ShipmentId.generate(), orderId);
        // In a real system, carrier selection would happen here
        newShipment.setCarrierName(CarrierName.FEDEX);
        newShipment.assignTrackingNumber(new TrackingNumber("trk-mock-12345"));
        shipmentRepository.save(newShipment);

        loadApplicationService.addShipmentToLoad(UNASSIGNED_LOAD_ID, newShipment.getId());
    }

    public void updateShipmentTracking(ShipmentId shipmentId, List<TrackingEvent> newEvents) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalStateException("Shipment not found: " + shipmentId));

        newEvents.forEach(shipment::addTrackingEvent);
        shipmentRepository.save(shipment);
    }
}