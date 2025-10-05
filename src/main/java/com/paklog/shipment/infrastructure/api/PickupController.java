package com.paklog.shipment.infrastructure.api;

import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.Pickup;
import com.paklog.shipment.infrastructure.api.gen.controller.PickupApi;
import com.paklog.shipment.infrastructure.api.gen.dto.PickupDetails;
import com.paklog.shipment.infrastructure.api.gen.dto.SchedulePickupRequest;
import com.paklog.shipment.infrastructure.api.mapper.LoadMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
public class PickupController implements PickupApi {

    private final LoadApplicationService loadApplicationService;
    private final LoadMapper loadMapper;

    public PickupController(LoadApplicationService loadApplicationService, LoadMapper loadMapper) {
        this.loadApplicationService = loadApplicationService;
        this.loadMapper = loadMapper;
    }

    @Override
    public ResponseEntity<Void> cancelPickup(UUID loadId, String ifMatch) {
        var load = loadApplicationService.getLoad(LoadId.of(loadId));
        var etag = buildEtag(load.getUpdatedAt());
        if (ifMatch != null && !ifMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }
        loadApplicationService.cancelPickup(LoadId.of(loadId));
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PickupDetails> getPickup(UUID loadId) {
        Pickup pickup = loadApplicationService.getPickup(LoadId.of(loadId));
        return ResponseEntity.ok(loadMapper.toDto(pickup));
    }

    @Override
    public ResponseEntity<PickupDetails> schedulePickup(UUID loadId, SchedulePickupRequest schedulePickupRequest, String ifMatch) {
        Load existingLoad = loadApplicationService.getLoad(LoadId.of(loadId));
        var currentEtag = buildEtag(existingLoad.getUpdatedAt());
        if (ifMatch != null && !ifMatch.equals(currentEtag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        }

        boolean isNewPickup = existingLoad.getPickup() == null;
        Load updatedLoad = loadApplicationService.schedulePickup(
                LoadId.of(loadId),
                schedulePickupRequest.getScheduledFor(),
                loadMapper.toDomain(schedulePickupRequest.getLocation()),
                schedulePickupRequest.getContactName(),
                schedulePickupRequest.getContactPhone(),
                schedulePickupRequest.getInstructions()
        );

        var responseBody = loadMapper.toDto(updatedLoad.getPickup());
        HttpStatus status = isNewPickup ? HttpStatus.CREATED : HttpStatus.OK;
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

    private String buildEtag(OffsetDateTime timestamp) {
        return timestamp != null ? "\"" + timestamp.toInstant().toEpochMilli() + "\"" : "\"0\"";
    }
}
