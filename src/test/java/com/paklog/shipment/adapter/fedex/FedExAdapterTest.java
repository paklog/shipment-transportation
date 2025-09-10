package com.paklog.shipment.adapter.fedex;

import com.paklog.shipment.adapter.ICarrierAdapter;
import com.paklog.shipment.domain.CarrierInfo;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.TrackingUpdate;
import com.paklog.shipment.domain.exception.CarrierException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FedExAdapterTest {

    @Mock
    private FedExApiClient fedExApiClient;

    @InjectMocks
    private FedExAdapter fedExAdapter;

    private String packageId;
    private String orderId;
    private String trackingNumberString;
    private TrackingNumber trackingNumber;
    private FedExShipmentResponse mockShipmentResponse;
    private FedExTrackingResponse mockTrackingResponse;

    @BeforeEach
    void setUp() {
        packageId = "pkg-123";
        orderId = "ord-456";
        trackingNumberString = "track-789";
        trackingNumber = TrackingNumber.of(trackingNumberString);
        mockShipmentResponse = new FedExShipmentResponse(trackingNumberString, new byte[]{});
        mockTrackingResponse = new FedExTrackingResponse("DELIVERED", "Delivered to front door", "New York", Instant.now());
    }

    @Test
    void testCreateShipment_Success() {
        // Arrange
        when(fedExApiClient.createShipment(any(FedExShipmentRequest.class))).thenReturn(mockShipmentResponse);

        // Act
        CarrierInfo carrierInfo = fedExAdapter.createShipment(packageId, orderId);

        // Assert
        assertNotNull(carrierInfo);
        assertEquals(trackingNumberString, carrierInfo.getTrackingNumber());
        verify(fedExApiClient, times(1)).createShipment(any(FedExShipmentRequest.class));
    }

    @Test
    void testCreateShipment_FedExApiClientThrowsException() {
        // Arrange
        when(fedExApiClient.createShipment(any(FedExShipmentRequest.class))).thenThrow(new RuntimeException("API error"));

        // Act & Assert
        CarrierException thrown = assertThrows(CarrierException.class, () -> {
            fedExAdapter.createShipment(packageId, orderId);
        });
        assertTrue(thrown.getMessage().contains("FedEx shipment creation failed"));
        verify(fedExApiClient, times(1)).createShipment(any(FedExShipmentRequest.class));
    }

    @Test
    void testGetTrackingStatus_Success() {
        // Arrange
        when(fedExApiClient.getTrackingStatus(any(FedExTrackingRequest.class))).thenReturn(mockTrackingResponse);

        // Act
        TrackingUpdate trackingUpdate = fedExAdapter.getTrackingStatus(trackingNumber);

        // Assert
        assertNotNull(trackingUpdate);
        assertEquals(mockTrackingResponse.getStatus(), trackingUpdate.getStatus());
        assertEquals(mockTrackingResponse.getStatusDescription(), trackingUpdate.getStatusDescription());
        assertEquals(mockTrackingResponse.getLocation(), trackingUpdate.getLocation());
        assertEquals(mockTrackingResponse.getLastUpdated(), trackingUpdate.getLastUpdated());
        verify(fedExApiClient, times(1)).getTrackingStatus(any(FedExTrackingRequest.class));
    }

    @Test
    void testGetTrackingStatus_FedExApiClientThrowsException() {
        // Arrange
        when(fedExApiClient.getTrackingStatus(any(FedExTrackingRequest.class))).thenThrow(new RuntimeException("API error"));

        // Act & Assert
        CarrierException thrown = assertThrows(CarrierException.class, () -> {
            fedExAdapter.getTrackingStatus(trackingNumber);
        });
        assertTrue(thrown.getMessage().contains("FedEx tracking failed"));
        verify(fedExApiClient, times(1)).getTrackingStatus(any(FedExTrackingRequest.class));
    }
}
