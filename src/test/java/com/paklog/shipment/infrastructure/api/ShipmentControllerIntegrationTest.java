package com.paklog.shipment.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.application.exception.ShipmentCreationException;
import com.paklog.shipment.application.exception.ShipmentNotFoundException;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.infrastructure.api.dto.CreateShipmentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.paklog.shipment.application.command.CreateShipmentCommand;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

@WebMvcTest(controllers = ShipmentController.class)
class ShipmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShipmentApplicationService shipmentService;

    @Test
    void testCreateShipment() throws Exception {
        // Arrange
        String packageId = "pkg-test-1";
        String orderId = "ord-test-1";
        Shipment mockShipment = Shipment.restore(
                ShipmentId.generate(),
                OrderId.of(orderId),
                CarrierName.FEDEX,
                TrackingNumber.of("TRK123"),
                ShipmentStatus.DISPATCHED,
                Instant.now(),
                Instant.now(),
                null,
                List.of()
        );
        when(shipmentService.createShipment(any(CreateShipmentCommand.class))).thenReturn(mockShipment);

        // Act & Assert
        mockMvc.perform(post("/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateShipmentRequest(packageId, orderId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNumber").value("TRK123"))
                .andExpect(jsonPath("$.shipmentId").exists());
    }

    @Test
    void testGetShipmentTracking() throws Exception {
        // Arrange
        String trackingNumber = "TRK123";
        Shipment mockShipment = Shipment.restore(
                ShipmentId.generate(),
                OrderId.of("ord-test-2"),
                CarrierName.UPS,
                TrackingNumber.of(trackingNumber),
                ShipmentStatus.DISPATCHED,
                Instant.now(),
                Instant.now(),
                null,
                List.of()
        );
        when(shipmentService.getShipmentTracking(trackingNumber)).thenReturn(mockShipment);

        // Act & Assert
        mockMvc.perform(get("/shipments/tracking/{trackingNumber}", trackingNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value(trackingNumber))
                .andExpect(jsonPath("$.trackingHistory").isArray());
    }

    @Test
    void testCreateShipmentValidationFailure() throws Exception {
        CreateShipmentRequest invalidRequest = new CreateShipmentRequest("", " ");

        mockMvc.perform(post("/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("validation_error"))
            .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void testGetShipmentTracking_NotFoundMappedTo404() throws Exception {
        String trackingNumber = "missing";
        when(shipmentService.getShipmentTracking(trackingNumber)).thenThrow(new ShipmentNotFoundException("Shipment not found"));

        mockMvc.perform(get("/shipments/tracking/{trackingNumber}", trackingNumber))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("shipment_not_found"));
    }

    @Test
    void testCreateShipment_CarrierFailureReturnsBadGateway() throws Exception {
        CreateShipmentRequest request = new CreateShipmentRequest("pkg", "order");
        when(shipmentService.createShipment(any(CreateShipmentCommand.class)))
            .thenThrow(new ShipmentCreationException("Carrier unavailable"));

        mockMvc.perform(post("/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.code").value("shipment_creation_failed"));
    }
}
