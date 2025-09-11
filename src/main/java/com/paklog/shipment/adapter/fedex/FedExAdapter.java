package com.paklog.shipment.adapter.fedex;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.domain.*;
import com.paklog.shipment.domain.exception.CarrierException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class FedExAdapter implements ICarrierAdapter {
    private final FedExApiClient fedExApiClient;

    public FedExAdapter(FedExApiClient fedExApiClient) {
        this.fedExApiClient = fedExApiClient;
    }

    @Override
    public String createShipment(com.paklog.shipment.domain.Package packageInfo) throws CarrierException {
        // This would call the real FedEx API
        return "trk-" + java.util.UUID.randomUUID().toString();
    }

    @Override
    public Optional<TrackingUpdate> getTrackingStatus(TrackingNumber trackingNumber) throws CarrierException {
        // This would call the real FedEx API
        return Optional.empty();
    }

    @Override
    public ShippingCost rateLoad(Load load) throws CarrierException {
        // Mock implementation: $10 per shipment + $0.50 per unit of weight
        BigDecimal shipmentCost = BigDecimal.TEN.multiply(new BigDecimal(load.getShipmentIds().size()));
        BigDecimal weightCost = new BigDecimal("0.50").multiply(load.getTotalWeight());
        BigDecimal total = shipmentCost.add(weightCost);
        return new ShippingCost(total, "USD", 3);
    }

    @Override
    public boolean tenderLoad(Load load) throws CarrierException {
        // Mock implementation: always successful
        System.out.printf("Tendering load %s to FedEx... SUCCESS%n", load.getId());
        return true;
    }

    @Override
    public String schedulePickup(Load load) throws CarrierException {
        // Mock implementation: return a fake confirmation number
        return "CONF-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public CarrierName getCarrierName() {
        return CarrierName.FEDEX;
    }
}
