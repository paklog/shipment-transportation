package com.paklog.shipment.domain.services;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.CarrierSelectionStrategy;
import com.paklog.shipment.domain.Package;
public class DefaultCarrierSelectionStrategy implements CarrierSelectionStrategy {

    @Override
    public CarrierName selectCarrier(Package packageDetails) {
        if (packageDetails == null) {
            throw new IllegalArgumentException("Package details cannot be null");
        }
        double weight = packageDetails.getWeight();
        if (weight < 0) {
            throw new IllegalArgumentException("Package weight cannot be negative");
        }
        if (weight < 5.0) {
            return CarrierName.FEDEX;
        }
        return CarrierName.UPS;
    }
}
