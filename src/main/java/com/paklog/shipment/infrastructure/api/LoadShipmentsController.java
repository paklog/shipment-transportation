package com.paklog.shipment.infrastructure.api;

import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.infrastructure.api.gen.controller.LoadShipmentsApi;
import com.paklog.shipment.infrastructure.api.gen.dto.Load;
import com.paklog.shipment.infrastructure.api.gen.dto.LoadShipmentAssignmentRequest;
import com.paklog.shipment.infrastructure.api.gen.dto.ShipmentCollection;
import com.paklog.shipment.infrastructure.api.mapper.LoadMapper;
import com.paklog.shipment.infrastructure.api.mapper.ShipmentMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
public class LoadShipmentsController implements LoadShipmentsApi {

    private final LoadApplicationService loadApplicationService;
    private final LoadMapper loadMapper;
    private final ShipmentMapper shipmentMapper;

    public LoadShipmentsController(LoadApplicationService loadApplicationService,
                                   LoadMapper loadMapper,
                                   ShipmentMapper shipmentMapper) {
        this.loadApplicationService = loadApplicationService;
        this.loadMapper = loadMapper;
        this.shipmentMapper = shipmentMapper;
    }

    @Override
    public ResponseEntity<ShipmentCollection> listShipmentsForLoad(UUID loadId, Integer page, Integer size) {
        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 20;
        var shipmentsPage = loadApplicationService.getShipmentsForLoad(LoadId.of(loadId), resolvedPage, resolvedSize);
        return ResponseEntity.ok(shipmentMapper.toDto(shipmentsPage));
    }

    @Override
    public ResponseEntity<Load> assignShipmentsToLoad(UUID loadId, LoadShipmentAssignmentRequest loadShipmentAssignmentRequest) {
        var updatedLoad = loadApplicationService.assignShipmentsToLoad(
                LoadId.of(loadId),
                loadShipmentAssignmentRequest.getShipmentIds()
        );
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .build()
                .toUri();
        return ResponseEntity.created(location)
                .body(loadMapper.toDto(updatedLoad));
    }

    @Override
    public ResponseEntity<Void> removeShipmentFromLoad(UUID loadId, UUID shipmentId) {
        loadApplicationService.removeShipmentFromLoad(LoadId.of(loadId), ShipmentId.of(shipmentId));
        return ResponseEntity.noContent().build();
    }
}
