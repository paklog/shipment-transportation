package com.paklog.shipment.application;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.application.command.CreateShipmentCommand;
import com.paklog.shipment.domain.CarrierInfo;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.exception.CarrierException;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import com.paklog.shipment.domain.services.CarrierSelectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock
    private ICarrierAdapter carrierAdapter;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private PackageRetrievalService packageRetrievalService;

    @Mock
    private CarrierSelectionService carrierSelectionService;

    @InjectMocks
    private ShipmentService shipmentService;

    private String packageId;
    private String orderId;
    private com.paklog.shipment.domain.Package mockPackage;
    private CarrierInfo mockCarrierInfo;
    private Shipment mockShipment;

    @BeforeEach
    void setUp() {
        packageId = "pkg-123";
        orderId = "ord-456";
        mockPackage = new com.paklog.shipment.domain.Package(10.0, 10.0, 10.0, 10.0, "BOX");
        mockCarrierInfo = new CarrierInfo("track-789", new byte[]{});
        ShipmentId shipmentId = ShipmentId.newId();
        mockShipment = Shipment.newShipment(shipmentId, OrderId.of(orderId), CarrierName.FEDEX);
        mockShipment.assignTrackingNumber(TrackingNumber.of("track-789"));
    }

    @Test
    void testCreateShipment_Success() {
        // Arrange
        when(packageRetrievalService.getPackageDetails(packageId)).thenReturn(mockPackage);
        when(carrierSelectionService.selectBestCarrier(mockPackage)).thenReturn(CarrierName.FEDEX);
        when(carrierAdapter.createShipment(packageId, orderId)).thenReturn(mockCarrierInfo);
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(mockShipment);

        // Act
        CreateShipmentCommand command = new CreateShipmentCommand(packageId, orderId);
        Shipment createdShipment = shipmentService.createShipment(command);

        // Assert
        assertNotNull(createdShipment);
        assertEquals(mockShipment, createdShipment);
        verify(packageRetrievalService, times(1)).getPackageDetails(packageId);
        verify(carrierSelectionService, times(1)).selectBestCarrier(mockPackage);
        verify(carrierAdapter, times(1)).createShipment(packageId, orderId);
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
    }

    @Test
    void testCreateShipment_PackageRetrievalThrowsException() {
        // Arrange
        when(packageRetrievalService.getPackageDetails(packageId)).thenThrow(new RuntimeException("Package not found"));

        // Act & Assert
        CreateShipmentCommand command = new CreateShipmentCommand(packageId, orderId);
        ShipmentService.ShipmentCreationException thrown = assertThrows(ShipmentService.ShipmentCreationException.class, () -> {
            shipmentService.createShipment(command);
        });
        assertTrue(thrown.getMessage().contains("Shipment creation failed"));
        verify(packageRetrievalService, times(1)).getPackageDetails(packageId);
        verify(carrierAdapter, never()).createShipment(anyString(), anyString());
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testCreateShipment_CarrierAdapterThrowsException() {
        // Arrange
        when(packageRetrievalService.getPackageDetails(packageId)).thenReturn(mockPackage);
        when(carrierSelectionService.selectBestCarrier(mockPackage)).thenReturn(CarrierName.FEDEX);
        when(carrierAdapter.createShipment(packageId, orderId)).thenThrow(new CarrierException("Carrier error", new RuntimeException("Carrier failure")));

        // Act & Assert
        CreateShipmentCommand command = new CreateShipmentCommand(packageId, orderId);
        CarrierException thrown = assertThrows(CarrierException.class, () -> {
            shipmentService.createShipment(command);
        });
        assertTrue(thrown.getMessage().contains("Shipment creation failed"));
        verify(packageRetrievalService, times(1)).getPackageDetails(packageId);
        verify(carrierAdapter, times(1)).createShipment(packageId, orderId);
        verify(shipmentRepository, never()).save(any(Shipment.class));
    }

    @Test
    void testCreateShipment_UnexpectedRuntimeIsWrapped() {
        when(packageRetrievalService.getPackageDetails(packageId)).thenReturn(mockPackage);
        when(carrierSelectionService.selectBestCarrier(mockPackage)).thenReturn(CarrierName.FEDEX);
        when(carrierAdapter.createShipment(packageId, orderId)).thenReturn(mockCarrierInfo);
        when(shipmentRepository.save(any(Shipment.class))).thenThrow(new IllegalStateException("Repository error"));

        CreateShipmentCommand command = new CreateShipmentCommand(packageId, orderId);
        ShipmentService.ShipmentCreationException thrown = assertThrows(ShipmentService.ShipmentCreationException.class, () -> shipmentService.createShipment(command));

        assertTrue(thrown.getMessage().contains("Shipment creation failed"));
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
    }

    @Test
    void testGetShipmentTracking_Success() {
        // Arrange
        TrackingNumber trackingNumber = TrackingNumber.of("track-123");
        when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.of(mockShipment));

        // Act
        Shipment foundShipment = shipmentService.getShipmentTracking(trackingNumber.getValue());

        // Assert
        assertNotNull(foundShipment);
        assertEquals(mockShipment, foundShipment);
        verify(shipmentRepository, times(1)).findByTrackingNumber(trackingNumber);
    }

    @Test
    void testGetShipmentTracking_NotFound() {
        // Arrange
        TrackingNumber trackingNumber = TrackingNumber.of("track-123");
        when(shipmentRepository.findByTrackingNumber(trackingNumber)).thenReturn(Optional.empty());

        // Act & Assert
        ShipmentService.ShipmentNotFoundException thrown = assertThrows(ShipmentService.ShipmentNotFoundException.class, () -> {
            shipmentService.getShipmentTracking(trackingNumber.getValue());
        });
        assertTrue(thrown.getMessage().contains("Shipment not found"));
        verify(shipmentRepository, times(1)).findByTrackingNumber(trackingNumber);
    }
}
