package com.paklog.shipment.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.shipment.TestFixtures;
import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadStatus;
import com.paklog.shipment.infrastructure.api.gen.dto.AssignCarrierRequest;
import com.paklog.shipment.infrastructure.api.mapper.LoadMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoadCarrierController.class)
@Import(LoadMapper.class)
class LoadCarrierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoadApplicationService loadApplicationService;

    @Test
    void assignCarrierReturnsPreconditionFailedOnMismatch() throws Exception {
        Load load = TestFixtures.sampleLoad(LoadStatus.PLANNED, CarrierName.FEDEX);
        when(loadApplicationService.getLoad(load.getId())).thenReturn(load);

        AssignCarrierRequest request = new AssignCarrierRequest()
                .carrierName(com.paklog.shipment.infrastructure.api.gen.dto.CarrierName.FEDEX);

        mockMvc.perform(put("/loads/{id}/carrier", load.getId().getValue())
                        .header("If-Match", "\"mismatch\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isPreconditionFailed());

        verify(loadApplicationService, never()).assignCarrier(any(), any(), any(), any(), any());
    }

    @Test
    void assignCarrierReturnsUpdatedLoad() throws Exception {
        Load current = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        when(loadApplicationService.getLoad(current.getId())).thenReturn(current);

        Load updated = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        updated.assignCarrier(CarrierName.FEDEX);
        when(loadApplicationService.assignCarrier(any(), any(), any(), any(), any())).thenReturn(updated);

        AssignCarrierRequest request = new AssignCarrierRequest()
                .carrierName(com.paklog.shipment.infrastructure.api.gen.dto.CarrierName.FEDEX);

        mockMvc.perform(put("/loads/{id}/carrier", current.getId().getValue())
                        .header("If-Match", quote(current.getUpdatedAt()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"));
    }

    @Test
    void unassignCarrierReturnsNoContent() throws Exception {
        Load load = TestFixtures.sampleLoad(LoadStatus.TENDERED, CarrierName.FEDEX);
        when(loadApplicationService.getLoad(load.getId())).thenReturn(load);

        mockMvc.perform(delete("/loads/{id}/carrier", load.getId().getValue())
                        .header("If-Match", quote(load.getUpdatedAt())))
                .andExpect(status().isNoContent());

        verify(loadApplicationService).unassignCarrier(load.getId());
    }

    private String quote(java.time.OffsetDateTime timestamp) {
        return "\"" + timestamp.toInstant().toEpochMilli() + "\"";
    }
}
