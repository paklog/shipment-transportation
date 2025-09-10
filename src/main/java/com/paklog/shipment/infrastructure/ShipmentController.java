package com.paklog.shipment.infrastructure;

import com.paklog.shipment.application.ShipmentService;
import com.paklog.shipment.domain.Shipment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.paklog.shipment.application.command.CreateShipmentCommand;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {
    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<Shipment> createShipment(@RequestBody CreateShipmentRequest request) {
        CreateShipmentCommand command = new CreateShipmentCommand(request.packageId, request.orderId);
        Shipment shipment = shipmentService.createShipment(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(shipment);
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<Shipment> trackShipment(@PathVariable String trackingNumber) {
        Shipment shipment = shipmentService.getShipmentTracking(trackingNumber);
        return ResponseEntity.ok(shipment);
    }

    static class CreateShipmentRequest {
        public String packageId;
        public String orderId;
    }
}