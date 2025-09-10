package com.paklog.shipment.domain.services;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.CarrierSelectionStrategy;
import com.paklog.shipment.domain.Package;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarrierSelectionServiceTest {

    @Mock
    private CarrierSelectionStrategy carrierSelectionStrategy;

    @InjectMocks
    private CarrierSelectionService carrierSelectionService;

    private Package mockPackage;

    @BeforeEach
    void setUp() {
        mockPackage = new Package(10.0, 10.0, 10.0, 10.0, "BOX");
    }

    @Test
    void testSelectCarrier() {
        // Arrange
        when(carrierSelectionStrategy.selectCarrier(mockPackage)).thenReturn(CarrierName.FEDEX);

        // Act
        CarrierName selectedCarrier = carrierSelectionService.selectBestCarrier(mockPackage);

        // Assert
        assertEquals(CarrierName.FEDEX, selectedCarrier);
    }
}
