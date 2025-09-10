package com.paklog.shipment.domain;

public interface CarrierSelectionStrategy {
    CarrierName selectCarrier(Package packageDetails);
}
