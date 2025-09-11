package com.paklog.shipment.infrastructure;

import com.paklog.shipment.application.BillOfLadingService;
import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShippingCost;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/loads")
public class LoadController {

    private final LoadApplicationService loadApplicationService;
    private final BillOfLadingService billOfLadingService;

    public LoadController(LoadApplicationService loadApplicationService, BillOfLadingService billOfLadingService) {
        this.loadApplicationService = loadApplicationService;
        this.billOfLadingService = billOfLadingService;
    }

    @PostMapping
    public ResponseEntity<Void> createLoad() {
        LoadId newLoadId = loadApplicationService.createLoad();
        return ResponseEntity.created(URI.create("/loads/" + newLoadId.toString())).build();
    }

    @PostMapping("/{loadId}/shipments")
    public ResponseEntity<Void> addShipmentToLoad(@PathVariable String loadId, @RequestBody AddShipmentRequest request) {
        loadApplicationService.addShipmentToLoad(LoadId.of(loadId), ShipmentId.of(request.shipmentId()));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{loadId}/carrier")
    public ResponseEntity<Void> assignCarrier(@PathVariable String loadId, @RequestBody AssignCarrierRequest request) {
        loadApplicationService.assignCarrierToLoad(LoadId.of(loadId), CarrierName.valueOf(request.carrierName()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{loadId}/rate")
    public ResponseEntity<ShippingCost> rateLoad(@PathVariable String loadId) {
        ShippingCost cost = loadApplicationService.rateLoad(LoadId.of(loadId));
        return ResponseEntity.ok(cost);
    }

    @PostMapping("/{loadId}/tender")
    public ResponseEntity<Void> tenderLoad(@PathVariable String loadId) {
        loadApplicationService.tenderLoad(LoadId.of(loadId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{loadId}/tender/response")
    public ResponseEntity<Void> handleTenderResponse(@PathVariable String loadId, @RequestBody TenderResponseRequest request) {
        loadApplicationService.handleTenderResponse(LoadId.of(loadId), request.accepted());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{loadId}/schedule-pickup")
    public ResponseEntity<PickupConfirmationResponse> schedulePickup(@PathVariable String loadId) {
        String confirmation = loadApplicationService.schedulePickup(LoadId.of(loadId));
        return ResponseEntity.ok(new PickupConfirmationResponse(confirmation));
    }

    @GetMapping(value = "/{loadId}/bol", produces = "text/plain")
    public ResponseEntity<String> getBillOfLading(@PathVariable String loadId) {
        String bol = billOfLadingService.generateBol(LoadId.of(loadId));
        return ResponseEntity.ok(bol);
    }

    @PostMapping("/{loadId}/ship-confirm")
    public ResponseEntity<Void> shipConfirm(@PathVariable String loadId) {
        loadApplicationService.shipConfirm(LoadId.of(loadId));
        return ResponseEntity.ok().build();
    }

    // DTOs
    public record AddShipmentRequest(String shipmentId) {}
    public record AssignCarrierRequest(String carrierName) {}
    public record TenderResponseRequest(boolean accepted) {}
    public record PickupConfirmationResponse(String confirmationNumber) {}
}
