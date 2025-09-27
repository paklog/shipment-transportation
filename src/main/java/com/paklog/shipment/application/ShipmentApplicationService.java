package com.paklog.shipment.application;

import com.paklog.shipment.application.command.CreateShipmentCommand;
import com.paklog.shipment.application.exception.ShipmentNotFoundException;
import com.paklog.shipment.domain.*;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShipmentApplicationService {

    private final ShipmentRepository shipmentRepository;
    private final LoadApplicationService loadApplicationService;
    private final MetricsService metricsService;

    private static final LoadId UNASSIGNED_LOAD_ID = LoadId.of("00000000-0000-0000-0000-000000000000");

    public ShipmentApplicationService(ShipmentRepository shipmentRepository, LoadApplicationService loadApplicationService, MetricsService metricsService) {
        this.shipmentRepository = shipmentRepository;
        this.loadApplicationService = loadApplicationService;
        this.metricsService = metricsService;
    }

    public Shipment createShipment(CreateShipmentCommand command) {
        Shipment newShipment = new Shipment(ShipmentId.generate(), command.getOrderId());
        // In a real system, carrier selection would happen here
        newShipment.setCarrierName(CarrierName.FEDEX);
        newShipment.assignTrackingNumber(new TrackingNumber("trk-mock-12345"));
        shipmentRepository.save(newShipment);

        metricsService.shipmentsCreated.increment(); // Increment metric

        loadApplicationService.addShipmentToLoad(UNASSIGNED_LOAD_ID, newShipment.getId());
        return newShipment;
    }

    public Shipment getShipmentTracking(String trackingNumber) {
        // This is a simplified implementation. In a real system, you'd have a proper tracking number index
        List<Shipment> allShipments = shipmentRepository.findAll();
        return allShipments.stream()
                .filter(shipment -> shipment.getTrackingNumber() != null &&
                       shipment.getTrackingNumber().getValue().equals(trackingNumber))
                .findFirst()
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with tracking number: " + trackingNumber));
    }

    public void updateShipmentTracking(ShipmentId shipmentId, List<TrackingEvent> newEvents) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalStateException("Shipment not found: " + shipmentId));

        newEvents.forEach(shipment::addTrackingEvent);
        shipmentRepository.save(shipment);
    }
}