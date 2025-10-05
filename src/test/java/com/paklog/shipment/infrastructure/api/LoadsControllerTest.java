package com.paklog.shipment.infrastructure.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paklog.shipment.TestFixtures;
import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.application.command.CreateLoadCommand;
import com.paklog.shipment.application.command.UpdateLoadCommand;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadStatus;
import com.paklog.shipment.infrastructure.api.gen.dto.CreateLoadRequest;
import com.paklog.shipment.infrastructure.api.gen.dto.Location;
import com.paklog.shipment.infrastructure.api.gen.dto.UpdateLoadRequest;
import com.paklog.shipment.infrastructure.api.mapper.LoadMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LoadsController.class)
@Import(LoadMapper.class)
class LoadsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoadApplicationService loadApplicationService;

    @Test
    void getLoadReturnsNotModifiedWhenEtagMatches() throws Exception {
        Load domainLoad = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        when(loadApplicationService.getLoad(domainLoad.getId())).thenReturn(domainLoad);
        String etag = quote(domainLoad.getUpdatedAt());

        mockMvc.perform(get("/loads/{id}", domainLoad.getId().getValue())
                        .header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    void getLoadReturnsBodyWhenEtagDiffers() throws Exception {
        Load domainLoad = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        when(loadApplicationService.getLoad(domainLoad.getId())).thenReturn(domainLoad);

        mockMvc.perform(get("/loads/{id}", domainLoad.getId().getValue()))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.id").value(domainLoad.getId().getValue().toString()))
                .andExpect(jsonPath("$.reference").value(domainLoad.getReference()));
    }

    @Test
    void listLoadsReturnsCollectionWithEtag() throws Exception {
        Load domainLoad = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        when(loadApplicationService.getLoads(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(domainLoad)));

        mockMvc.perform(get("/loads")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.items[0].id").value(domainLoad.getId().getValue().toString()));
    }

    @Test
    void createLoadPassesCommandToService() throws Exception {
        Load domainLoad = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        when(loadApplicationService.createLoad(any(CreateLoadCommand.class))).thenReturn(domainLoad);

        CreateLoadRequest request = new CreateLoadRequest()
                .reference("REF-999")
                .shipments(Set.of(UUID.randomUUID()))
                .origin(apiLocation())
                .destination(apiLocation())
                .requestedPickupDate(domainLoad.getRequestedPickupDate())
                .requestedDeliveryDate(domainLoad.getRequestedDeliveryDate())
                .notes("Notes");

        mockMvc.perform(post("/loads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().exists("ETag"));

        ArgumentCaptor<CreateLoadCommand> captor = ArgumentCaptor.forClass(CreateLoadCommand.class);
        verify(loadApplicationService).createLoad(captor.capture());
        CreateLoadCommand command = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("REF-999", command.reference());
    }

    @Test
    void updateLoadHonoursPrecondition() throws Exception {
        Load domainLoad = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        when(loadApplicationService.getLoad(domainLoad.getId())).thenReturn(domainLoad);
        when(loadApplicationService.updateLoad(any(), any(UpdateLoadCommand.class))).thenReturn(domainLoad);

        UpdateLoadRequest request = new UpdateLoadRequest()
                .status(com.paklog.shipment.infrastructure.api.gen.dto.LoadStatus.BOOKED);

        mockMvc.perform(patch("/loads/{id}", domainLoad.getId().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .header("If-Match", quote(domainLoad.getUpdatedAt())))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"));
    }

    @Test
    void deleteLoadReturnsPreconditionFailedOnEtagMismatch() throws Exception {
        Load domainLoad = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        when(loadApplicationService.getLoad(domainLoad.getId())).thenReturn(domainLoad);

        mockMvc.perform(delete("/loads/{id}", domainLoad.getId().getValue())
                        .header("If-Match", "\"mismatch\""))
                .andExpect(status().isPreconditionFailed());

        verify(loadApplicationService, never()).deleteLoad(any());
    }

    @Test
    void deleteLoadRemovesWhenEtagsMatch() throws Exception {
        Load domainLoad = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        when(loadApplicationService.getLoad(domainLoad.getId())).thenReturn(domainLoad);

        mockMvc.perform(delete("/loads/{id}", domainLoad.getId().getValue())
                        .header("If-Match", quote(domainLoad.getUpdatedAt())))
                .andExpect(status().isNoContent());

        verify(loadApplicationService).deleteLoad(domainLoad.getId());
    }

    @Test
    void updateLoadReturnsPreconditionFailedOnMismatch() throws Exception {
        Load domainLoad = TestFixtures.sampleLoad(LoadStatus.PLANNED, null);
        when(loadApplicationService.getLoad(domainLoad.getId())).thenReturn(domainLoad);

        UpdateLoadRequest request = new UpdateLoadRequest()
                .status(com.paklog.shipment.infrastructure.api.gen.dto.LoadStatus.BOOKED);

        mockMvc.perform(patch("/loads/{id}", domainLoad.getId().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                        .header("If-Match", "\"mismatch\""))
                .andExpect(status().isPreconditionFailed());

        verify(loadApplicationService, never()).updateLoad(any(), any());
    }

    private String quote(OffsetDateTime timestamp) {
        return "\"" + timestamp.toInstant().toEpochMilli() + "\"";
    }

    private Location apiLocation() {
        return new Location()
                .name("Warehouse")
                .addressLine1("123 Main St")
                .city("Portland")
                .stateOrProvince("OR")
                .postalCode("97201")
                .country("US");
    }
}
