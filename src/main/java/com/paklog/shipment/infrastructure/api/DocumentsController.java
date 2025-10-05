package com.paklog.shipment.infrastructure.api;

import com.paklog.shipment.application.LoadApplicationService;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.infrastructure.api.gen.controller.DocumentsApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DocumentsController implements DocumentsApi {

    private final LoadApplicationService loadApplicationService;

    public DocumentsController(LoadApplicationService loadApplicationService) {
        this.loadApplicationService = loadApplicationService;
    }

    @Override
    public ResponseEntity<String> getBillOfLading(UUID loadId) {
        return ResponseEntity.ok(loadApplicationService.getBillOfLading(LoadId.of(loadId)));
    }
}
