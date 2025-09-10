package com.paklog.shipment.application;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.application.command.CreateShipmentCommand;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.CarrierInfo;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.exception.CarrierException;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import com.paklog.shipment.util.CarrierSelectionUtil;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShipmentService {
    private final ICarrierAdapter carrierAdapter;
    private final ShipmentRepository shipmentRepository;
    private final PackageRetrievalService packageRetrievalService;

    public ShipmentService(ICarrierAdapter carrierAdapter,
                           ShipmentRepository shipmentRepository,
                           PackageRetrievalService packageRetrievalService) {
        this.carrierAdapter = carrierAdapter;
        this.shipmentRepository = shipmentRepository;
        this.packageRetrievalService = packageRetrievalService;
    }

    @Transactional
    public Shipment createShipment(@Valid CreateShipmentCommand command) {
        try {
            // Get package details
            com.paklog.shipment.domain.Package packageDetail = packageRetrievalService.getPackageDetails(command.getPackageId());

            // Determine best carrier using utility
            CarrierName carrierName = CarrierSelectionUtil.selectBestCarrier(packageDetail);

            // Create shipment with selected carrier
            CarrierInfo carrierInfo = carrierAdapter.createShipment(command.getPackageId(), command.getOrderId());

            // Create domain shipment object
            Shipment shipment = new Shipment(
                ShipmentId.newId(),
                OrderId.of(command.getOrderId()),
                TrackingNumber.of(carrierInfo.getTrackingNumber()),
                carrierName,
                ShipmentStatus.CREATED
            );

            // Save to repository
            return shipmentRepository.save(shipment);
        } catch (Exception e) {
            throw new CarrierException("Shipment creation failed for package: " + command.getPackageId(), e);
        }
    }

    public Shipment getShipmentTracking(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(TrackingNumber.of(trackingNumber))
            .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found for tracking number: " + trackingNumber));
    }

    public static class ShipmentNotFoundException extends RuntimeException {
        public ShipmentNotFoundException(String message) {
            super(message);
        }
    }
}