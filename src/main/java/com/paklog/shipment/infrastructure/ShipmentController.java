package com.paklog.shipment.infrastructure;

import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentRepository shipmentRepository;
    private static final LoadId UNASSIGNED_LOAD_ID = LoadId.of("00000000-0000-0000-0000-000000000000");

    public ShipmentController(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    @GetMapping
    public List<Shipment> getUnassignedShipments(@RequestParam String status) {
        if ("unassigned".equalsIgnoreCase(status)) {
            return shipmentRepository.findByLoadId(UNASSIGNED_LOAD_ID);
        }
        return List.of();
    }
}
