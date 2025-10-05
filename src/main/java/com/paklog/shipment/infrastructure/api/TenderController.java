package com.paklog.shipment.infrastructure.api;

import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.Tender;
import com.paklog.shipment.domain.TenderStatus;
import com.paklog.shipment.infrastructure.api.gen.controller.TenderApi;
import com.paklog.shipment.infrastructure.api.gen.dto.TenderDecisionRequest;
import com.paklog.shipment.infrastructure.api.gen.dto.TenderRequest;
import com.paklog.shipment.infrastructure.api.mapper.LoadMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
public class TenderController implements TenderApi {

    private final LoadApplicationService loadApplicationService;
    private final LoadMapper loadMapper;

    public TenderController(LoadApplicationService loadApplicationService, LoadMapper loadMapper) {
        this.loadApplicationService = loadApplicationService;
        this.loadMapper = loadMapper;
    }

    @Override
    public ResponseEntity<Void> cancelTender(UUID loadId, String ifMatch) {
        var load = loadApplicationService.getLoad(LoadId.of(loadId));
        var etag = buildEtag(load.getUpdatedAt());
        if (ifMatch != null && !ifMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }
        loadApplicationService.cancelTender(LoadId.of(loadId));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<com.paklog.shipment.infrastructure.api.gen.dto.Tender> getTender(UUID loadId) {
        var load = loadApplicationService.getLoad(LoadId.of(loadId));
        var tender = load.getTender();
        return ResponseEntity.ok()
                .eTag(buildEtag(load.getUpdatedAt()))
                .body(loadMapper.toDto(tender));
    }

    @Override
    public ResponseEntity<com.paklog.shipment.infrastructure.api.gen.dto.Tender> tenderLoad(UUID loadId, TenderRequest tenderRequest, String ifMatch) {
        var load = loadApplicationService.getLoad(LoadId.of(loadId));
        var currentEtag = buildEtag(load.getUpdatedAt());
        if (ifMatch != null && !ifMatch.equals(currentEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }

        TenderStatus previousStatus = load.getTender() != null ? load.getTender().status() : TenderStatus.NOT_TENDERED;
        Load updatedLoad = loadApplicationService.tenderLoad(LoadId.of(loadId), tenderRequest.getExpiresAt(), tenderRequest.getNotes());
        var responseBody = loadMapper.toDto(updatedLoad.getTender());

        HttpStatus status = previousStatus == TenderStatus.NOT_TENDERED ? HttpStatus.CREATED : HttpStatus.OK;
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status)
                .eTag(buildEtag(updatedLoad.getUpdatedAt()));
        if (status == HttpStatus.CREATED) {
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequestUri()
                    .build()
                    .toUri();
            builder = builder.location(location);
        }
        return builder.body(responseBody);
    }

    @Override
    public ResponseEntity<com.paklog.shipment.infrastructure.api.gen.dto.Tender> recordTenderDecision(UUID loadId, TenderDecisionRequest tenderDecisionRequest, String ifMatch) {
        var load = loadApplicationService.getLoad(LoadId.of(loadId));
        var currentEtag = buildEtag(load.getUpdatedAt());
        if (ifMatch != null && !ifMatch.equals(currentEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }

        Tender.Decision decision = Tender.Decision.valueOf(tenderDecisionRequest.getDecision().getValue());
        var updatedLoad = loadApplicationService.recordTenderDecision(
                LoadId.of(loadId),
                decision,
                tenderDecisionRequest.getRespondedBy(),
                tenderDecisionRequest.getReason()
        );
        return ResponseEntity.ok()
                .eTag(buildEtag(updatedLoad.getUpdatedAt()))
                .body(loadMapper.toDto(updatedLoad.getTender()));
    }

    private String buildEtag(OffsetDateTime timestamp) {
        return timestamp != null ? "\"" + timestamp.toInstant().toEpochMilli() + "\"" : "\"0\"";
    }
}
