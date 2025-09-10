package com.paklog.shipment.util;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Package;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarrierSelectionUtilTest {

    @Test
    void testSelectBestCarrier_FedEx() {
        // Arrange
        Package smallPackage = new Package(4.0, 10.0, 10.0, 10.0, "BOX");

        // Act
        CarrierName carrier = CarrierSelectionUtil.selectBestCarrier(smallPackage);

        // Assert
        assertEquals(CarrierName.FEDEX, carrier);
    }

    @Test
    void testSelectBestCarrier_UPS() {
        // Arrange
        Package largePackage = new Package(6.0, 10.0, 10.0, 10.0, "BOX");

        // Act
        CarrierName carrier = CarrierSelectionUtil.selectBestCarrier(largePackage);

        // Assert
        assertEquals(CarrierName.UPS, carrier);
    }

    @Test
    void testSelectBestCarrier_EdgeCase_Boundary() {
        // Arrange
        Package boundaryPackage = new Package(5.0, 10.0, 10.0, 10.0, "BOX");

        // Act
        CarrierName carrier = CarrierSelectionUtil.selectBestCarrier(boundaryPackage);

        // Assert
        assertEquals(CarrierName.UPS, carrier);
    }

    @Test
    void testSelectBestCarrier_EdgeCase_ZeroWeight() {
        // Arrange
        Package zeroWeightPackage = new Package(0.0, 10.0, 10.0, 10.0, "BOX");

        // Act
        CarrierName carrier = CarrierSelectionUtil.selectBestCarrier(zeroWeightPackage);

        // Assert
        assertEquals(CarrierName.FEDEX, carrier);
    }
}
