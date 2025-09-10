package com.paklog.shipment.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.shipment.ShipmentTransportationApplication;
import com.paklog.shipment.application.ShipmentService;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.domain.TrackingNumber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
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
        Shipment mockShipment = new Shipment(
                ShipmentId.newId(),
                OrderId.of(orderId),
                TrackingNumber.of("TRK123"),
                CarrierName.FEDEX,
                ShipmentStatus.CREATED
        );
        when(shipmentService.createShipment(any(CreateShipmentCommand.class))).thenReturn(mockShipment);

        // Act & Assert
        mockMvc.perform(post("/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateShipmentRequest(packageId, orderId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value("TRK123"));
    }

    @Test
    void testGetShipmentTracking() throws Exception {
        // Arrange
        String trackingNumber = "TRK123";
        Shipment mockShipment = new Shipment(
                ShipmentId.newId(),
                OrderId.of("ord-test-2"),
                TrackingNumber.of(trackingNumber),
                CarrierName.UPS,
                ShipmentStatus.IN_TRANSIT
        );
        when(shipmentService.getShipmentTracking(trackingNumber)).thenReturn(mockShipment);

        // Act & Assert
        mockMvc.perform(get("/shipments/tracking/{trackingNumber}", trackingNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(trackingNumber));
    }

    // Helper class for request body
    static class CreateShipmentRequest {
        public String packageId;
        public String orderId;

        public CreateShipmentRequest(String packageId, String orderId) {
            this.packageId = packageId;
            this.orderId = orderId;
        }
    }
}
