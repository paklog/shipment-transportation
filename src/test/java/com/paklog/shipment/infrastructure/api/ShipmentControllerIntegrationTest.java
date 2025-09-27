package com.paklog.shipment.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.shipment.ShipmentTransportationApplication;
import com.paklog.shipment.application.ShipmentService;
import com.paklog.shipment.application.ShipmentService.ShipmentCreationException;
import com.paklog.shipment.application.ShipmentService.ShipmentNotFoundException;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.infrastructure.api.dto.CreateShipmentRequest;
import com.paklog.shipment.domain.exception.CarrierException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest(classes = ShipmentTransportationApplication.class)
@AutoConfigureMockMvc
class ShipmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShipmentService shipmentService;

    @Test
    void testCreateShipment() throws Exception {
        // Arrange
        String packageId = "pkg-test-1";
        String orderId = "ord-test-1";
        Shipment mockShipment = Shipment.newShipment(ShipmentId.newId(), OrderId.of(orderId), CarrierName.FEDEX);
        mockShipment.assignTrackingNumber(TrackingNumber.of("TRK123"));
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
        Shipment mockShipment = Shipment.newShipment(ShipmentId.newId(), OrderId.of("ord-test-2"), CarrierName.UPS);
        mockShipment.assignTrackingNumber(TrackingNumber.of(trackingNumber));
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
            .thenThrow(new CarrierException("Carrier unavailable", new RuntimeException("failure")));

        mockMvc.perform(post("/shipments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.code").value("shipment_creation_failed"));
    }
}
