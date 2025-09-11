package com.paklog.shipment.application;

import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.repository.ILoadRepository;
import org.springframework.stereotype.Service;

@Service
public class BillOfLadingService {

    private final ILoadRepository loadRepository;

    public BillOfLadingService(ILoadRepository loadRepository) {
        this.loadRepository = loadRepository;
    }

    public String generateBol(LoadId loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new IllegalStateException("Load not found: " + loadId));

        // This is a simplified text representation of a BOL.
        // A real implementation would use a PDF library and contain much more detail.
        StringBuilder bol = new StringBuilder();
        bol.append("========== BILL OF LADING ==========\n");
        bol.append(String.format("LOAD ID: %s\n", load.getId()));
        bol.append(String.format("CARRIER: %s\n", load.getCarrierName()));
        bol.append(String.format("STATUS: %s\n", load.getStatus()));
        bol.append("-------------------------------------\n");
        bol.append(String.format("SHIPMENT COUNT: %d\n", load.getShipmentIds().size()));
        bol.append(String.format("TOTAL WEIGHT: %s\n", load.getTotalWeight()));
        bol.append("-------------------------------------\n");
        bol.append("SHIPMENTS:\n");
        load.getShipmentIds().forEach(shipmentId -> bol.append(String.format("- %s\n", shipmentId)));
        bol.append("====================================\n");

        return bol.toString();
    }
}
