package com.paklog.shipment.adapter.fedex;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.application.MetricsService;
import com.paklog.shipment.domain.CarrierInfo;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.ShippingCost;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.TrackingUpdate;
import com.paklog.shipment.domain.exception.CarrierException;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class FedExAdapter implements ICarrierAdapter {
    private final FedExApiClient fedExApiClient;
    private final MetricsService metricsService;

    public FedExAdapter(FedExApiClient fedExApiClient, MetricsService metricsService) {
        this.fedExApiClient = fedExApiClient;
        this.metricsService = metricsService;
    }

    @Override
    public CarrierInfo createShipment(com.paklog.shipment.domain.Package packageInfo,
                                      OrderId orderId,
                                      String packageId) throws CarrierException {
        Timer.Sample sample = metricsService.startCarrierApiTimer();
        String status = "success";
        try {
            FedExShipmentRequest request = new FedExShipmentRequest(packageId, orderId.getValue());
            FedExShipmentResponse response = fedExApiClient.createShipment(request);
            return new CarrierInfo(response.getTrackingNumber(), response.getLabelData(), CarrierName.FEDEX);
        } catch (RuntimeException ex) {
            status = "error";
            throw new CarrierException(ex.getMessage(), CarrierName.FEDEX.name(), null, ex);
        } finally {
            metricsService.recordCarrierApiCall(sample, "FedEx", "createShipment", status);
        }
    }

    @Override
    public Optional<TrackingUpdate> getTrackingStatus(TrackingNumber trackingNumber) throws CarrierException {
        // This would call the real FedEx API
        return Optional.empty();
    }

    @Override
    public ShippingCost rateLoad(Load load) throws CarrierException {
        Timer.Sample sample = metricsService.startCarrierApiTimer();
        String status = "success";
        try {
            // Mock implementation: $10 per shipment + $0.50 per unit of weight
            // Using the new Load properties
            BigDecimal shipmentCost = BigDecimal.TEN.multiply(new BigDecimal(load.getShipmentIds().size()));
            // For now, we'll use a dummy weight as totalWeight was removed.
            // In a real scenario, this would come from the shipments within the load.
            BigDecimal dummyWeight = BigDecimal.valueOf(load.getShipmentIds().size() * 10); // 10 units per shipment
            BigDecimal weightCost = new BigDecimal("0.50").multiply(dummyWeight);
            BigDecimal total = shipmentCost.add(weightCost);
            return new ShippingCost(total, "USD", 3);
        } catch (RuntimeException ex) {
            status = "error";
            throw ex;
        } finally {
            metricsService.recordCarrierApiCall(sample, "FedEx", "rateLoad", status);
        }
    }

    @Override
    public boolean tenderLoad(Load load) throws CarrierException {
        Timer.Sample sample = metricsService.startCarrierApiTimer();
        String status = "success";
        try {
            // Mock implementation: always successful
            System.out.printf("Tendering load %s to FedEx... SUCCESS%n", load.getId());
            return true;
        } catch (RuntimeException ex) {
            status = "error";
            throw ex;
        } finally {
            metricsService.recordCarrierApiCall(sample, "FedEx", "tenderLoad", status);
        }
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
