package com.paklog.shipment.domain.services;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.CarrierSelectionStrategy;
import com.paklog.shipment.domain.Package;
import org.springframework.stereotype.Component;

@Component
public class DefaultCarrierSelectionStrategy implements CarrierSelectionStrategy {

    @Override
    public CarrierName selectCarrier(Package packageDetails) {
        // Default implementation: always select FEDEX
        return CarrierName.FEDEX;
    }
}
