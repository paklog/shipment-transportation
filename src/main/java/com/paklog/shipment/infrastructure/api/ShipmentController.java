package com.paklog.shipment.infrastructure.api;

import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.application.command.CreateShipmentCommand;
import com.paklog.shipment.infrastructure.api.dto.CreateShipmentRequest;
import com.paklog.shipment.infrastructure.api.dto.ShipmentResponse;
import com.paklog.shipment.domain.ShipmentId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/shipments")
@Validated
public class ShipmentController {
    private final ShipmentApplicationService shipmentService;

    public ShipmentController(ShipmentApplicationService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(@Valid @RequestBody CreateShipmentRequest request) {
        CreateShipmentCommand command = new CreateShipmentCommand(request.getPackageId(), request.getOrderId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ShipmentResponseMapper.toResponse(shipmentService.createShipment(command)));
    }

    @GetMapping("/{shipmentId}/tracking")
    public ResponseEntity<ShipmentResponse> trackShipment(@PathVariable String shipmentId) {
        return ResponseEntity.ok(ShipmentResponseMapper.toResponse(shipmentService.getShipmentTracking(ShipmentId.of(shipmentId))));
    }
}
