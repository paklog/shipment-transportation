package com.paklog.shipment.domain;

// This is a simplified entity for demonstration.
// A full implementation would have more complex rules.
public class LoadTemplate {

    private final String destinationCity;
    private final CarrierName preferredCarrier;

    public LoadTemplate(String destinationCity, CarrierName preferredCarrier) {
        this.destinationCity = destinationCity;
        this.preferredCarrier = preferredCarrier;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public CarrierName getPreferredCarrier() {
        return preferredCarrier;
    }
}
