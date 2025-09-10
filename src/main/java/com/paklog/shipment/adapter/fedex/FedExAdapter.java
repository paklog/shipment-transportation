package com.paklog.shipment.adapter.fedex;

import com.paklog.shipment.domain.CarrierInfo;
import com.paklog.shipment.domain.TrackingUpdate;
import com.paklog.shipment.domain.TrackingUpdate;
import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.domain.exception.CarrierException;
import org.springframework.stereotype.Component;

@Component
public class FedExAdapter implements ICarrierAdapter {
    private final FedExApiClient fedExApiClient;

    public FedExAdapter(FedExApiClient fedExApiClient) {
        this.fedExApiClient = fedExApiClient;
    }

    @Override
    public CarrierInfo createShipment(String packageId, String orderId) {
        try {
            FedExShipmentRequest request = new FedExShipmentRequest(packageId, orderId);
            FedExShipmentResponse response = fedExApiClient.createShipment(request);
            return new CarrierInfo(response.getTrackingNumber(), response.getLabelData());
        } catch (Exception e) {
            throw new CarrierException("FedEx shipment creation failed for package: " + packageId, e);
        }
    }

    @Override
    public TrackingUpdate getTrackingStatus(com.paklog.shipment.domain.TrackingNumber trackingNumber) {
        try {
            FedExTrackingRequest request = new FedExTrackingRequest(trackingNumber.getValue());
            FedExTrackingResponse response = fedExApiClient.getTrackingStatus(request);
            return new TrackingUpdate(
                response.getStatus(),
                response.getStatusDescription(),
                response.getLocation(),
                response.getLastUpdated()
            );
        } catch (Exception e) {
            throw new CarrierException("FedEx tracking failed for: " + trackingNumber.getValue(), e);
        }
    }
}