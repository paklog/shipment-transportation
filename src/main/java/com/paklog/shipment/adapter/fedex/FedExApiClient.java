package com.paklog.shipment.adapter.fedex;

import com.paklog.shipment.adapter.fedex.exception.FedExApiException;
import com.paklog.shipment.adapter.fedex.exception.FedExAuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class FedExApiClient {
    public FedExShipmentResponse createShipment(FedExShipmentRequest request) {
        // Simulate an actual API call with potential failures
        if (request.getPackageId().equals("fail-api")) {
            throw new FedExApiException("Simulated FedEx API error during shipment creation");
        }
        if (request.getPackageId().equals("fail-auth")) {
            throw new FedExAuthenticationException("Simulated FedEx authentication failure");
        }
        byte[] labelBytes = ("LABEL-" + request.getPackageId()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new FedExShipmentResponse("dummy-tracking-number-" + request.getPackageId(), labelBytes);
    }

    public FedExTrackingResponse getTrackingStatus(FedExTrackingRequest request) {
        // Simulate an actual API call with potential failures
        if (request.getTrackingNumber().equals("fail-api")) {
            throw new FedExApiException("Simulated FedEx API error during tracking status retrieval");
        }
        if (request.getTrackingNumber().equals("fail-auth")) {
            throw new FedExAuthenticationException("Simulated FedEx authentication failure");
        }
        return new FedExTrackingResponse("IN_TRANSIT", "Shipment is on its way", "Some location", java.time.Instant.now());
    }
}
