package com.paklog.shipment.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.shipment.TestFixtures;
import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadStatus;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.infrastructure.api.gen.dto.LoadShipmentAssignmentRequest;
import com.paklog.shipment.infrastructure.api.mapper.LoadMapper;
import com.paklog.shipment.infrastructure.api.mapper.ShipmentMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoadShipmentsController.class)
@Import({LoadMapper.class, ShipmentMapper.class})
class LoadShipmentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoadApplicationService loadApplicationService;

    @Test
    void listShipmentsForLoadReturnsPage() throws Exception {
        Shipment shipment = TestFixtures.sampleShipment(com.paklog.shipment.domain.ShipmentStatus.IN_TRANSIT, com.paklog.shipment.domain.CarrierName.FEDEX);
        when(loadApplicationService.getShipmentsForLoad(any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(shipment)));

        mockMvc.perform(get("/loads/{id}/shipments", UUID.randomUUID())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id.value").value(shipment.getId().getValue().toString()));
    }

    @Test
    void assignShipmentsReturnsCreatedWithLocation() throws Exception {
        Load load = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        when(loadApplicationService.assignShipmentsToLoad(any(), any())).thenReturn(load);

        LoadShipmentAssignmentRequest request = new LoadShipmentAssignmentRequest()
                .shipmentIds(Set.of(UUID.randomUUID()));

        mockMvc.perform(post("/loads/{id}/shipments", load.getId().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(load.getId().getValue().toString()))
                .andExpect(jsonPath("$.shipments").isArray());
    }

    @Test
    void removeShipmentReturnsNoContent() throws Exception {
        UUID loadId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();

        mockMvc.perform(delete("/loads/{loadId}/shipments/{shipmentId}", loadId, shipmentId))
                .andExpect(status().isNoContent());

        ArgumentCaptor<com.paklog.shipment.domain.LoadId> loadCaptor = ArgumentCaptor.forClass(com.paklog.shipment.domain.LoadId.class);
        ArgumentCaptor<ShipmentId> shipmentCaptor = ArgumentCaptor.forClass(ShipmentId.class);
        verify(loadApplicationService).removeShipmentFromLoad(loadCaptor.capture(), shipmentCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(loadId, loadCaptor.getValue().getValue());
        org.junit.jupiter.api.Assertions.assertEquals(shipmentId, shipmentCaptor.getValue().getValue());
    }
}
