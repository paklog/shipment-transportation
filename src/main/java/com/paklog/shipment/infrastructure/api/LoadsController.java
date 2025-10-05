
package com.paklog.shipment.infrastructure.api;

import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.application.command.CreateLoadCommand;
import com.paklog.shipment.application.command.UpdateLoadCommand;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.infrastructure.api.gen.controller.LoadsApi;
import com.paklog.shipment.infrastructure.api.gen.dto.CarrierName;
import com.paklog.shipment.infrastructure.api.gen.dto.CreateLoadRequest;
import com.paklog.shipment.infrastructure.api.gen.dto.Load;
import com.paklog.shipment.infrastructure.api.gen.dto.LoadCollection;
import com.paklog.shipment.infrastructure.api.gen.dto.LoadStatus;
import com.paklog.shipment.infrastructure.api.gen.dto.UpdateLoadRequest;
import com.paklog.shipment.infrastructure.api.mapper.LoadMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
public class LoadsController implements LoadsApi {

    private final LoadApplicationService loadApplicationService;
    private final LoadMapper loadMapper;


    public LoadsController(LoadApplicationService loadApplicationService, LoadMapper loadMapper) {
        this.loadApplicationService = loadApplicationService;
        this.loadMapper = loadMapper;
    }


    @Override
    public ResponseEntity<Load> createLoad(CreateLoadRequest createLoadRequest) {
        CreateLoadCommand command = new CreateLoadCommand(
                createLoadRequest.getReference(),
                loadMapper.toShipmentIdSet(createLoadRequest.getShipments()),
                loadMapper.toDomain(createLoadRequest.getOrigin()),
                loadMapper.toDomain(createLoadRequest.getDestination()),
                createLoadRequest.getRequestedPickupDate(),
                createLoadRequest.getRequestedDeliveryDate(),
                createLoadRequest.getNotes()
        );
        var domainLoad = loadApplicationService.createLoad(command);
        Load loadResponse = loadMapper.toDto(domainLoad);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(loadResponse.getId())
                .toUri();
        return ResponseEntity.created(location)
                .eTag(buildEtag(domainLoad.getUpdatedAt()))
                .body(loadResponse);
    }

    @Override
    public ResponseEntity<Void> deleteLoad(UUID loadId, String ifMatch) {
        var domainLoad = loadApplicationService.getLoad(LoadId.of(loadId));
        var currentEtag = buildEtag(domainLoad.getUpdatedAt());
        if (ifMatch != null && !ifMatch.equals(currentEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }
        loadApplicationService.deleteLoad(LoadId.of(loadId));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Load> getLoad(UUID loadId, String ifNoneMatch) {
        var domainLoad = loadApplicationService.getLoad(LoadId.of(loadId));
        var etag = buildEtag(domainLoad.getUpdatedAt());
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .body(loadMapper.toDto(domainLoad));
    }

    @Override
    public ResponseEntity<LoadCollection> listLoads(LoadStatus status, CarrierName carrierName, Integer page, Integer size) {
        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 20;
        var domainLoads = loadApplicationService.getLoads(
                loadMapper.toDomain(status),
                loadMapper.toDomain(carrierName),
                resolvedPage,
                resolvedSize
        );
        var etag = buildCollectionEtag(domainLoads);
        return ResponseEntity.ok()
                .eTag(etag)
                .body(loadMapper.toDto(domainLoads));
    }

    @Override
    public ResponseEntity<Load> updateLoad(UUID loadId, UpdateLoadRequest updateLoadRequest, String ifMatch) {
        UpdateLoadCommand command = new UpdateLoadCommand(
                updateLoadRequest.getReference(),
                updateLoadRequest.getRequestedPickupDate(),
                updateLoadRequest.getRequestedDeliveryDate(),
                loadMapper.toDomain(updateLoadRequest.getStatus()),
                updateLoadRequest.getNotes()
        );
        var domainLoad = loadApplicationService.getLoad(LoadId.of(loadId));
        var currentEtag = buildEtag(domainLoad.getUpdatedAt());
        if (ifMatch != null && !ifMatch.equals(currentEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }
        var updatedLoad = loadApplicationService.updateLoad(LoadId.of(loadId), command);
        return ResponseEntity.ok()
                .eTag(buildEtag(updatedLoad.getUpdatedAt()))
                .body(loadMapper.toDto(updatedLoad));
    }

    private String buildEtag(OffsetDateTime timestamp) {
        return timestamp != null ? "\"" + timestamp.toInstant().toEpochMilli() + "\"" : "\"0\"";
    }

    private String buildCollectionEtag(org.springframework.data.domain.Page<com.paklog.shipment.domain.Load> page) {
        return page.getContent().stream()
                .map(com.paklog.shipment.domain.Load::getUpdatedAt)
                .max(OffsetDateTime::compareTo)
                .map(this::buildEtag)
                .orElse("\"0\"");
    }
}
