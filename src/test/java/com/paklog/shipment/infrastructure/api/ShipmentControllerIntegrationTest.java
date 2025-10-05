package com.paklog.shipment.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.domain.TrackingEvent;
import com.paklog.shipment.domain.TrackingNumber;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShipmentController.class)
@Import(com.paklog.shipment.infrastructure.api.mapper.ShipmentMapper.class)
class ShipmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ShipmentApplicationService shipmentService;

    @Test
    void getShipmentReturnsEtagAndPayload() throws Exception {
        Shipment shipment = sampleShipment();
        when(shipmentService.getShipment(any(ShipmentId.class))).thenReturn(shipment);

        mockMvc.perform(get("/shipments/{shipmentId}", shipment.getId().getValue()))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.id.value").value(shipment.getId().getValue().toString()))
                .andExpect(jsonPath("$.status").value(shipment.getStatus().name()))
                .andExpect(jsonPath("$.trackingNumber.value").value(shipment.getTrackingNumber().getValue()));
    }

    @Test
    void getShipmentHonoursIfNoneMatch() throws Exception {
        Shipment shipment = sampleShipment();
        when(shipmentService.getShipment(any(ShipmentId.class))).thenReturn(shipment);

        String etag = quote(shipment.getLastUpdatedAt().toInstant().toEpochMilli());

        mockMvc.perform(get("/shipments/{shipmentId}", shipment.getId().getValue())
                        .header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void listShipmentsReturnsCollection() throws Exception {
        Shipment shipment = sampleShipment();
        when(shipmentService.getShipments(any(), any(), anyInt(), anyInt()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(shipment)));

        mockMvc.perform(get("/shipments")
                        .param("page", "0")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.items[0].id.value").value(shipment.getId().getValue().toString()));
    }

    private Shipment sampleShipment() {
        ShipmentId shipmentId = ShipmentId.generate();
        OrderId orderId = OrderId.of("ORD-123");
        CarrierName carrier = CarrierName.FEDEX;
        TrackingNumber trackingNumber = TrackingNumber.of("TRK-12345");
        OffsetDateTime now = OffsetDateTime.now();
        Shipment shipment = Shipment.restore(
                shipmentId,
                orderId,
                carrier,
                trackingNumber,
                "label".getBytes(),
                ShipmentStatus.IN_TRANSIT,
                now.minusDays(1),
                now.minusHours(20),
                null,
                List.of(new TrackingEvent("IN_TRANSIT", "Package departed origin", "Portland, OR", now.minusHours(2), "DEPARTED", "")),
                LoadId.of("00000000-0000-0000-0000-000000000000"),
                now
        );
        return shipment;
    }

    private String quote(long value) {
        return "\"" + value + "\"";
    }
}
