
package com.paklog.shipment.infrastructure.api;

import com.paklog.shipment.application.ShipmentApplicationService;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.infrastructure.api.gen.controller.ShipmentsApi;
import com.paklog.shipment.infrastructure.api.gen.dto.CarrierName;
import com.paklog.shipment.infrastructure.api.gen.dto.Shipment;
import com.paklog.shipment.infrastructure.api.gen.dto.ShipmentCollection;
import com.paklog.shipment.infrastructure.api.gen.dto.ShipmentStatus;
import com.paklog.shipment.infrastructure.api.mapper.ShipmentMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ShipmentController implements ShipmentsApi {

    private final ShipmentApplicationService shipmentService;
    private final ShipmentMapper shipmentMapper;

    public ShipmentController(ShipmentApplicationService shipmentService, ShipmentMapper shipmentMapper) {
        this.shipmentService = shipmentService;
        this.shipmentMapper = shipmentMapper;
    }

    @Override
    public ResponseEntity<Shipment> getShipment(UUID shipmentId, String ifNoneMatch) {
        var domainShipment = shipmentService.getShipment(ShipmentId.of(shipmentId));
        var etag = buildEtag(domainShipment.getLastUpdatedAt());
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .body(shipmentMapper.toDto(domainShipment));
    }

    @Override
    public ResponseEntity<ShipmentCollection> listShipments(ShipmentStatus status, CarrierName carrierName, Integer page, Integer size) {
        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 20;
        var domainShipments = shipmentService.getShipments(
                shipmentMapper.toDomain(status),
                shipmentMapper.toDomain(carrierName),
                resolvedPage,
                resolvedSize
        );
        var etag = domainShipments.getContent().stream()
                .map(com.paklog.shipment.domain.Shipment::getLastUpdatedAt)
                .max(java.time.OffsetDateTime::compareTo)
                .map(this::buildEtag)
                .orElse("\"0\"");
        return ResponseEntity.ok()
                .eTag(etag)
                .body(shipmentMapper.toDto(domainShipments));
    }

    private String buildEtag(java.time.OffsetDateTime timestamp) {
        return timestamp != null ? "\"" + timestamp.toInstant().toEpochMilli() + "\"" : "\"0\"";
    }
}
