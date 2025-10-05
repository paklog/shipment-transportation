package com.paklog.shipment.infrastructure.api;

import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.infrastructure.api.gen.controller.LoadRatingApi;
import com.paklog.shipment.infrastructure.api.gen.dto.ShippingCost;
import com.paklog.shipment.infrastructure.api.mapper.LoadMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class LoadRatingController implements LoadRatingApi {

    private final LoadApplicationService loadApplicationService;
    private final LoadMapper loadMapper;

    public LoadRatingController(LoadApplicationService loadApplicationService, LoadMapper loadMapper) {
        this.loadApplicationService = loadApplicationService;
        this.loadMapper = loadMapper;
    }

    @Override
    public ResponseEntity<ShippingCost> rateLoad(UUID loadId) {
        var shippingCost = loadApplicationService.rateLoad(LoadId.of(loadId));
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(loadMapper.toDto(shippingCost));
    }
}
