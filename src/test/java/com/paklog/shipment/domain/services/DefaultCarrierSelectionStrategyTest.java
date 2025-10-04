package com.paklog.shipment.domain.services;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Package;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultCarrierSelectionStrategyTest {

    private final DefaultCarrierSelectionStrategy strategy = new DefaultCarrierSelectionStrategy();

    @Test
    void selectsFedExForLightPackages() {
        CarrierName carrier = strategy.selectCarrier(new Package("pkg-1", 4.5, 10, 10, 10, "BOX"));
        assertEquals(CarrierName.FEDEX, carrier);
    }

    @Test
    void selectsUpsForHeavyPackages() {
        CarrierName carrier = strategy.selectCarrier(new Package("pkg-2", 6.0, 10, 10, 10, "BOX"));
        assertEquals(CarrierName.UPS, carrier);
    }

    @Test
    void treatsZeroWeightAsLight() {
        CarrierName carrier = strategy.selectCarrier(new Package("pkg-3", 0.0, 10, 10, 10, "BOX"));
        assertEquals(CarrierName.FEDEX, carrier);
    }

    @Test
    void rejectsNegativeWeight() {
        assertThrows(IllegalArgumentException.class, () -> strategy.selectCarrier(new Package("pkg-4", -1.0, 10, 10, 10, "BOX")));
    }

    @Test
    void rejectsNullPackage() {
        assertThrows(IllegalArgumentException.class, () -> strategy.selectCarrier(null));
    }
}
