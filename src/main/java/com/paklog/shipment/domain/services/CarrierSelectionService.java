package com.paklog.shipment.domain.services;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.CarrierSelectionStrategy;
import com.paklog.shipment.domain.Package;
public class CarrierSelectionService {
    private final CarrierSelectionStrategy carrierSelectionStrategy;

    public CarrierSelectionService(CarrierSelectionStrategy carrierSelectionStrategy) {
        this.carrierSelectionStrategy = carrierSelectionStrategy;
    }

    public CarrierName selectBestCarrier(Package packageDetail) {
        return carrierSelectionStrategy.selectCarrier(packageDetail);
    }
}
