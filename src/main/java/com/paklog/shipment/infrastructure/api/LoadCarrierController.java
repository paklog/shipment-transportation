package com.paklog.shipment.infrastructure.api;

import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.infrastructure.api.gen.controller.LoadCarrierApi;
import com.paklog.shipment.infrastructure.api.gen.dto.AssignCarrierRequest;
import com.paklog.shipment.infrastructure.api.gen.dto.Load;
import com.paklog.shipment.infrastructure.api.mapper.LoadMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
public class LoadCarrierController implements LoadCarrierApi {

    private final LoadApplicationService loadApplicationService;
    private final LoadMapper loadMapper;

    public LoadCarrierController(LoadApplicationService loadApplicationService, LoadMapper loadMapper) {
        this.loadApplicationService = loadApplicationService;
        this.loadMapper = loadMapper;
    }

    @Override
    public ResponseEntity<Load> assignCarrier(UUID loadId, AssignCarrierRequest assignCarrierRequest, String ifMatch) {
        var currentLoad = loadApplicationService.getLoad(LoadId.of(loadId));
        var currentEtag = buildEtag(currentLoad.getUpdatedAt());
        if (ifMatch != null && !ifMatch.equals(currentEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }

        var updatedLoad = loadApplicationService.assignCarrier(
                LoadId.of(loadId),
                loadMapper.toDomain(assignCarrierRequest.getCarrierName()),
                assignCarrierRequest.getScac(),
                assignCarrierRequest.getContactName(),
                assignCarrierRequest.getContactPhone()
        );

        return ResponseEntity.ok()
                .eTag(buildEtag(updatedLoad.getUpdatedAt()))
                .body(loadMapper.toDto(updatedLoad));
    }

    @Override
    public ResponseEntity<Void> unassignCarrier(UUID loadId, String ifMatch) {
        var currentLoad = loadApplicationService.getLoad(LoadId.of(loadId));
        var currentEtag = buildEtag(currentLoad.getUpdatedAt());
        if (ifMatch != null && !ifMatch.equals(currentEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }
        loadApplicationService.unassignCarrier(LoadId.of(loadId));
        return ResponseEntity.noContent().build();
    }

    private String buildEtag(OffsetDateTime timestamp) {
        return timestamp != null ? "\"" + timestamp.toInstant().toEpochMilli() + "\"" : "\"0\"";
    }
}
