package com.paklog.shipment.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackageNameConverterTest {

    @Test
    void testToExpectedPackage() {
        // Arrange
        String packageName = "com.paklog.shipment.domain";

        // Act
        String expectedPackage = PackageNameConverter.toExpectedPackage(packageName);

        // Assert
        assertEquals("main.java.com.paklog.shipment.domain", expectedPackage);
    }

    @Test
    void testToActualPackage() {
        // Arrange
        String expectedPackage = "main.java.com.paklog.shipment.domain";

        // Act
        String actualPackage = PackageNameConverter.toActualPackage(expectedPackage);

        // Assert
        assertEquals("com.paklog.shipment.domain", actualPackage);
    }
}
