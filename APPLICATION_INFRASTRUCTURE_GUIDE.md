# Application & Infrastructure Implementation Guide

This guide provides detailed implementation examples for application services, infrastructure adapters, and external interfaces (Tasks 25-55).

## Application Layer (Tasks 29-34)

### ShipmentApplicationService (Task 30)
```java
package com.example.shipment.application.service;

import com.example.shipment.domain.model.aggregate.Shipment;
import com.example.shipment.domain.model.valueobject.*;
import com.example.shipment.domain.repository.ShipmentRepository;
import com.example.shipment.domain.service.CarrierSelectionService;
import com.example.shipment.application.port.ShipmentEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShipmentApplicationService {
    private final CarrierSelectionService carrierSelectionService;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventPublisher eventPublisher;
    
    public ShipmentApplicationService(
            CarrierSelectionService carrierSelectionService,
            ShipmentRepository shipmentRepository,
            ShipmentEventPublisher eventPublisher) {
        this.carrierSelectionService = carrierSelectionService;
        this.shipmentRepository = shipmentRepository;
        this.eventPublisher = eventPublisher;
    }
    
    public ShipmentId createShipmentFromPackage(Package packageInfo) {
        // 1. Check if shipment already exists for this order
        Optional<Shipment> existingShipment = shipmentRepository.findByOrderId(packageInfo.orderId());
        if (existingShipment.isPresent()) {
            throw new IllegalStateException("Shipment already exists for order: " + packageInfo.orderId());
        }
        
        // 2. Select best carrier using domain service
        CarrierSelectionResult selection = carrierSelectionService.selectBestCarrier(packageInfo);
        
        // 3. Create shipment with selected carrier
        CarrierInfo carrierInfo = selection.selectedCarrier().createShipment(packageInfo);
        
        // 4. Create and save aggregate
        Shipment shipment = new Shipment(packageInfo.orderId(), packageInfo, carrierInfo);
        Shipment savedShipment = shipmentRepository.save(shipment);
        
        // 5. Publish domain event via application port
        eventPublisher.publishShipmentDispatched(savedShipment);
        
        return savedShipment.getShipmentId();
    }
    
    public void updateShipmentTracking(ShipmentId shipmentId, List<TrackingEvent> newEvents) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentId));
        
        for (TrackingEvent event : newEvents) {
            shipment.addTrackingEvent(event);
        }
        
        shipmentRepository.save(shipment);
        
        // Publish delivery event if shipment was delivered
        if (shipment.isDelivered()) {
            eventPublisher.publishShipmentDelivered(shipment);
        }
    }
}
```

### TrackingApplicationService (Task 31)
```java
package com.example.shipment.application.service;

import com.example.shipment.domain.model.aggregate.Shipment;
import com.example.shipment.domain.model.valueobject.ShipmentId;
import com.example.shipment.domain.repository.ShipmentRepository;
import com.example.shipment.application.dto.ShipmentTrackingDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrackingApplicationService {
    private final ShipmentRepository shipmentRepository;
    
    public TrackingApplicationService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }
    
    public ShipmentTrackingDto getShipmentTracking(ShipmentId shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentId));
        
        return ShipmentTrackingDto.fromDomain(shipment);
    }
    
    public List<ShipmentTrackingDto> getAllInTransitShipments() {
        List<Shipment> inTransitShipments = shipmentRepository.findAllInTransit();
        return inTransitShipments.stream()
            .map(ShipmentTrackingDto::fromDomain)
            .collect(Collectors.toList());
    }
}
```

### Application DTOs
```java
// ShipmentTrackingDto (Task 33)
package com.example.shipment.application.dto;

import com.example.shipment.domain.model.aggregate.Shipment;
import com.example.shipment.domain.model.entity.TrackingEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShipmentTrackingDto(
    UUID shipmentId,
    UUID orderId,
    String status,
    String carrierName,
    String trackingNumber,
    Instant createdAt,
    Instant dispatchedAt,
    Instant deliveredAt,
    List<TrackingEventDto> trackingHistory
) {
    public static ShipmentTrackingDto fromDomain(Shipment shipment) {
        return new ShipmentTrackingDto(
            shipment.getShipmentId().value(),
            shipment.getOrderId().value(),
            shipment.getStatus().getDisplayName(),
            shipment.getCarrierName().getDisplayName(),
            shipment.getTrackingNumber().value(),
            shipment.getCreatedAt(),
            shipment.getDispatchedAt(),
            shipment.getDeliveredAt(),
            shipment.getTrackingHistory().stream()
                .map(TrackingEventDto::fromDomain)
                .toList()
        );
    }
}

// TrackingEventDto
public record TrackingEventDto(
    Instant timestamp,
    String statusDescription,
    LocationDto location
) {
    public static TrackingEventDto fromDomain(TrackingEvent event) {
        return new TrackingEventDto(
            event.getTimestamp(),
            event.getStatusDescription(),
            event.getLocation() != null ? LocationDto.fromDomain(event.getLocation()) : null
        );
    }
}

// LocationDto  
public record LocationDto(
    String city,
    String stateOrRegion,
    String postalCode,
    String countryCode
) {
    public static LocationDto fromDomain(Location location) {
        return new LocationDto(
            location.city(),
            location.stateOrRegion(),
            location.postalCode(),
            location.countryCode()
        );
    }
}
```

### Application Ports
```java
// ShipmentEventPublisher port interface
package com.example.shipment.application.port;

import com.example.shipment.domain.model.aggregate.Shipment;

public interface ShipmentEventPublisher {
    void publishShipmentDispatched(Shipment shipment);
    void publishShipmentDelivered(Shipment shipment);
}
```

## Infrastructure Layer - Carrier Integration (Tasks 25-28)

### FedExApiClient (Task 25)
```java
package com.example.shipment.infrastructure.carrier.fedex;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Component
public class FedExApiClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String accountNumber;
    
    public FedExApiClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${carriers.fedex.api-url}") String baseUrl,
            @Value("${carriers.fedex.api-key}") String apiKey,
            @Value("${carriers.fedex.account-number}") String accountNumber) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.accountNumber = accountNumber;
    }
    
    public FedExShipmentResponse createShipment(FedExShipmentRequest request) throws FedExApiException {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<FedExShipmentRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<FedExShipmentResponse> response = restTemplate.exchange(
                baseUrl + "/ship/v1/shipments",
                HttpMethod.POST,
                entity,
                FedExShipmentResponse.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new FedExApiException("Failed to create shipment: " + response.getStatusCode());
            }
            
            return response.getBody();
            
        } catch (RestClientException e) {
            throw new FedExApiException("Error communicating with FedEx API", e);
        }
    }
    
    public FedExTrackingResponse getTrackingInfo(FedExTrackingRequest request) throws FedExApiException {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<FedExTrackingRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<FedExTrackingResponse> response = restTemplate.exchange(
                baseUrl + "/track/v1/trackingnumbers",
                HttpMethod.POST,
                entity,
                FedExTrackingResponse.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new FedExApiException("Failed to get tracking info: " + response.getStatusCode());
            }
            
            return response.getBody();
            
        } catch (RestClientException e) {
            throw new FedExApiException("Error communicating with FedEx API", e);
        }
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-locale", "en_US");
        headers.set("Authorization", "Bearer " + apiKey);
        return headers;
    }
}
```

### FedExAdapter (Task 26)
```java
package com.example.shipment.infrastructure.carrier.fedex;

import com.example.shipment.domain.model.valueobject.*;
import com.example.shipment.domain.model.entity.TrackingEvent;
import com.example.shipment.domain.port.ICarrierAdapter;
import com.example.shipment.domain.model.exception.CarrierException;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.List;

@Component
public class FedExAdapter implements ICarrierAdapter {
    private final FedExApiClient fedExApiClient;
    private final FedExRequestMapper requestMapper;
    private final FedExResponseMapper responseMapper;
    
    public FedExAdapter(
            FedExApiClient fedExApiClient,
            FedExRequestMapper requestMapper,
            FedExResponseMapper responseMapper) {
        this.fedExApiClient = fedExApiClient;
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
    }
    
    @Override
    public CarrierInfo createShipment(Package packageInfo) throws CarrierException {
        try {
            FedExShipmentRequest request = requestMapper.toFedExShipmentRequest(packageInfo);
            FedExShipmentResponse response = fedExApiClient.createShipment(request);
            
            return responseMapper.toCarrierInfo(response);
            
        } catch (FedExApiException e) {
            throw new CarrierException(
                "Failed to create FedEx shipment: " + e.getMessage(),
                getCarrierName().getDisplayName(),
                e.getErrorCode(),
                e
            );
        }
    }
    
    @Override
    public Optional<TrackingUpdate> getTrackingStatus(TrackingNumber trackingNumber) throws CarrierException {
        try {
            FedExTrackingRequest request = requestMapper.toFedExTrackingRequest(trackingNumber);
            FedExTrackingResponse response = fedExApiClient.getTrackingInfo(request);
            
            if (response.getOutput() == null || response.getOutput().getCompleteTrackResults().isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(responseMapper.toTrackingUpdate(response));
            
        } catch (FedExApiException e) {
            throw new CarrierException(
                "Failed to get FedEx tracking info: " + e.getMessage(),
                getCarrierName().getDisplayName(),
                e.getErrorCode(),
                e
            );
        }
    }
    
    @Override
    public CarrierName getCarrierName() {
        return CarrierName.FEDEX;
    }
    
    @Override
    public ShippingCost estimateShippingCost(Package packageInfo) throws CarrierException {
        // Implementation would call FedEx rate API
        // For demo purposes, return a fixed cost
        return new ShippingCost(
            new BigDecimal("25.99"),
            "USD",
            3
        );
    }
}
```

### FedEx DTOs (Tasks 27-28)
```java
// FedExShipmentRequest (Task 27)
package com.example.shipment.infrastructure.carrier.fedex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FedExShipmentRequest {
    @JsonProperty("labelResponseOptions")
    private String labelResponseOptions;
    
    @JsonProperty("requestedShipment")
    private RequestedShipment requestedShipment;
    
    @JsonProperty("accountNumber")
    private AccountNumber accountNumber;
    
    // Getters, setters, constructors...
    
    public static class RequestedShipment {
        @JsonProperty("shipper")
        private Address shipper;
        
        @JsonProperty("recipients")
        private List<Recipient> recipients;
        
        @JsonProperty("serviceType")
        private String serviceType;
        
        @JsonProperty("packagingType")
        private String packagingType;
        
        @JsonProperty("requestedPackageLineItems")
        private List<RequestedPackageLineItem> requestedPackageLineItems;
        
        // Getters, setters...
    }
    
    public static class RequestedPackageLineItem {
        @JsonProperty("weight")
        private Weight weight;
        
        @JsonProperty("dimensions")
        private Dimensions dimensions;
        
        // Getters, setters...
        
        public static class Weight {
            @JsonProperty("units")
            private String units;
            
            @JsonProperty("value")
            private double value;
            
            // Getters, setters...
        }
        
        public static class Dimensions {
            @JsonProperty("length")
            private int length;
            
            @JsonProperty("width") 
            private int width;
            
            @JsonProperty("height")
            private int height;
            
            @JsonProperty("units")
            private String units;
            
            // Getters, setters...
        }
    }
}

// FedExShipmentResponse
package com.example.shipment.infrastructure.carrier.fedex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FedExShipmentResponse {
    @JsonProperty("output")
    private Output output;
    
    public Output getOutput() { return output; }
    public void setOutput(Output output) { this.output = output; }
    
    public static class Output {
        @JsonProperty("transactionShipments")
        private List<TransactionShipment> transactionShipments;
        
        public List<TransactionShipment> getTransactionShipments() {
            return transactionShipments;
        }
        
        public void setTransactionShipments(List<TransactionShipment> transactionShipments) {
            this.transactionShipments = transactionShipments;
        }
    }
    
    public static class TransactionShipment {
        @JsonProperty("masterTrackingNumber")
        private String masterTrackingNumber;
        
        @JsonProperty("pieceResponses")
        private List<PieceResponse> pieceResponses;
        
        // Getters, setters...
    }
    
    public static class PieceResponse {
        @JsonProperty("baseLabel")
        private BaseLabel baseLabel;
        
        // Getters, setters...
        
        public static class BaseLabel {
            @JsonProperty("label")
            private String label; // Base64 encoded label
            
            // Getters, setters...
        }
    }
}

// FedExTrackingRequest (Task 28)
package com.example.shipment.infrastructure.carrier.fedex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FedExTrackingRequest {
    @JsonProperty("includeDetailedScans")
    private boolean includeDetailedScans = true;
    
    @JsonProperty("trackingInfo")
    private List<TrackingInfo> trackingInfo;
    
    // Constructors, getters, setters...
    
    public static class TrackingInfo {
        @JsonProperty("trackingNumberInfo")
        private TrackingNumberInfo trackingNumberInfo;
        
        // Getters, setters...
    }
    
    public static class TrackingNumberInfo {
        @JsonProperty("trackingNumber")
        private String trackingNumber;
        
        // Getters, setters...
    }
}

// FedExTrackingResponse
package com.example.shipment.infrastructure.carrier.fedex.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FedExTrackingResponse {
    @JsonProperty("output")
    private Output output;
    
    public Output getOutput() { return output; }
    public void setOutput(Output output) { this.output = output; }
    
    public static class Output {
        @JsonProperty("completeTrackResults")
        private List<CompleteTrackResult> completeTrackResults;
        
        // Getters, setters...
    }
    
    public static class CompleteTrackResult {
        @JsonProperty("trackingNumber")
        private String trackingNumber;
        
        @JsonProperty("scanEvents")
        private List<ScanEvent> scanEvents;
        
        @JsonProperty("deliveryDetails")
        private DeliveryDetails deliveryDetails;
        
        // Getters, setters...
    }
    
    public static class ScanEvent {
        @JsonProperty("date")
        private String date;
        
        @JsonProperty("eventDescription")
        private String eventDescription;
        
        @JsonProperty("scanLocation")
        private ScanLocation scanLocation;
        
        // Getters, setters...
    }
    
    public static class ScanLocation {
        @JsonProperty("city")
        private String city;
        
        @JsonProperty("stateOrProvinceCode")
        private String stateOrProvinceCode;
        
        @JsonProperty("postalCode")
        private String postalCode;
        
        @JsonProperty("countryCode")
        private String countryCode;
        
        // Getters, setters...
    }
}
```

### FedEx Mappers
```java
// FedExRequestMapper
package com.example.shipment.infrastructure.carrier.fedex.mapper;

import com.example.shipment.domain.model.valueobject.Package;
import com.example.shipment.domain.model.valueobject.TrackingNumber;
import com.example.shipment.infrastructure.carrier.fedex.dto.*;
import org.springframework.stereotype.Component;

@Component
public class FedExRequestMapper {
    
    public FedExShipmentRequest toFedExShipmentRequest(Package packageInfo) {
        FedExShipmentRequest request = new FedExShipmentRequest();
        request.setLabelResponseOptions("URL_ONLY");
        
        FedExShipmentRequest.RequestedShipment shipment = new FedExShipmentRequest.RequestedShipment();
        shipment.setServiceType("FEDEX_GROUND");
        shipment.setPackagingType("YOUR_PACKAGING");
        
        // Map package dimensions and weight
        FedExShipmentRequest.RequestedPackageLineItem packageLineItem = 
            new FedExShipmentRequest.RequestedPackageLineItem();
        
        FedExShipmentRequest.RequestedPackageLineItem.Weight weight = 
            new FedExShipmentRequest.RequestedPackageLineItem.Weight();
        weight.setUnits(packageInfo.weight().unit().name());
        weight.setValue(packageInfo.weight().value().doubleValue());
        packageLineItem.setWeight(weight);
        
        FedExShipmentRequest.RequestedPackageLineItem.Dimensions dimensions = 
            new FedExShipmentRequest.RequestedPackageLineItem.Dimensions();
        dimensions.setLength(packageInfo.dimensions().length().intValue());
        dimensions.setWidth(packageInfo.dimensions().width().intValue());
        dimensions.setHeight(packageInfo.dimensions().height().intValue());
        dimensions.setUnits(packageInfo.dimensions().unit().name());
        packageLineItem.setDimensions(dimensions);
        
        shipment.setRequestedPackageLineItems(List.of(packageLineItem));
        request.setRequestedShipment(shipment);
        
        return request;
    }
    
    public FedExTrackingRequest toFedExTrackingRequest(TrackingNumber trackingNumber) {
        FedExTrackingRequest request = new FedExTrackingRequest();
        request.setIncludeDetailedScans(true);
        
        FedExTrackingRequest.TrackingInfo trackingInfo = new FedExTrackingRequest.TrackingInfo();
        FedExTrackingRequest.TrackingNumberInfo numberInfo = new FedExTrackingRequest.TrackingNumberInfo();
        numberInfo.setTrackingNumber(trackingNumber.value());
        trackingInfo.setTrackingNumberInfo(numberInfo);
        
        request.setTrackingInfo(List.of(trackingInfo));
        
        return request;
    }
}

// FedExResponseMapper  
package com.example.shipment.infrastructure.carrier.fedex.mapper;

import com.example.shipment.domain.model.valueobject.*;
import com.example.shipment.domain.model.entity.TrackingEvent;
import com.example.shipment.infrastructure.carrier.fedex.dto.*;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Component
public class FedExResponseMapper {
    
    public CarrierInfo toCarrierInfo(FedExShipmentResponse response) {
        FedExShipmentResponse.TransactionShipment shipment = 
            response.getOutput().getTransactionShipments().get(0);
        
        TrackingNumber trackingNumber = new TrackingNumber(shipment.getMasterTrackingNumber());
        
        // Extract label data (assuming base64 encoded)
        byte[] labelData = Base64.getDecoder().decode(
            shipment.getPieceResponses().get(0).getBaseLabel().getLabel()
        );
        
        return new CarrierInfo(trackingNumber, labelData, CarrierName.FEDEX);
    }
    
    public TrackingUpdate toTrackingUpdate(FedExTrackingResponse response) {
        FedExTrackingResponse.CompleteTrackResult result = 
            response.getOutput().getCompleteTrackResults().get(0);
        
        List<TrackingEvent> events = result.getScanEvents().stream()
            .map(this::toTrackingEvent)
            .sorted((e1, e2) -> e1.getTimestamp().compareTo(e2.getTimestamp()))
            .toList();
        
        TrackingEvent latestEvent = events.get(events.size() - 1);
        boolean isDelivered = latestEvent.isDeliveryEvent();
        
        return new TrackingUpdate(latestEvent, isDelivered, events);
    }
    
    private TrackingEvent toTrackingEvent(FedExTrackingResponse.ScanEvent scanEvent) {
        Instant timestamp = parseTimestamp(scanEvent.getDate());
        String description = scanEvent.getEventDescription();
        
        Location location = null;
        if (scanEvent.getScanLocation() != null) {
            location = new Location(
                scanEvent.getScanLocation().getCity(),
                scanEvent.getScanLocation().getStateOrProvinceCode(),
                scanEvent.getScanLocation().getPostalCode(),
                scanEvent.getScanLocation().getCountryCode()
            );
        }
        
        return new TrackingEvent(timestamp, description, location);
    }
    
    private Instant parseTimestamp(String dateString) {
        // Parse FedEx date format to Instant
        LocalDateTime localDateTime = LocalDateTime.parse(dateString, 
            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }
}
```

## Web Layer (Task 32, 34)

### ShipmentController (Task 32)
```java
package com.example.shipment.adapter.web;

import com.example.shipment.application.service.TrackingApplicationService;
import com.example.shipment.application.dto.ShipmentTrackingDto;
import com.example.shipment.domain.model.valueobject.ShipmentId;
import com.example.shipment.adapter.web.dto.ShipmentTrackingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {
    private final TrackingApplicationService trackingApplicationService;
    
    public ShipmentController(TrackingApplicationService trackingApplicationService) {
        this.trackingApplicationService = trackingApplicationService;
    }
    
    @GetMapping("/{shipmentId}/tracking")
    @Operation(
        summary = "Get Shipment Tracking History",
        description = "Retrieve the complete tracking history for a specific shipment"
    )
    @ApiResponse(responseCode = "200", description = "Tracking history retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Shipment not found")
    public ResponseEntity<ShipmentTrackingResponse> getShipmentTracking(
            @PathVariable UUID shipmentId) {
        
        ShipmentId id = ShipmentId.from(shipmentId.toString());
        ShipmentTrackingDto tracking = trackingApplicationService.getShipmentTracking(id);
        
        ShipmentTrackingResponse response = ShipmentTrackingResponse.fromDto(tracking);
        return ResponseEntity.ok(response);
    }
}
```

### Global Exception Handler (Task 34)
```java
package com.example.shipment.adapter.web.exception;

import com.example.shipment.domain.model.exception.*;
import com.example.shipment.application.exception.ShipmentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ShipmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleShipmentNotFound(ShipmentNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            UUID.randomUUID().toString(),
            "SHIPMENT_NOT_FOUND",
            ex.getMessage(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(CarrierException.class)
    public ResponseEntity<ErrorResponse> handleCarrierException(CarrierException ex) {
        ErrorResponse error = new ErrorResponse(
            UUID.randomUUID().toString(),
            "CARRIER_ERROR",
            "Carrier integration failed: " + ex.getMessage(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    @ExceptionHandler(InvalidShipmentStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidShipmentState(InvalidShipmentStateException ex) {
        ErrorResponse error = new ErrorResponse(
            UUID.randomUUID().toString(),
            "INVALID_SHIPMENT_STATE",
            ex.getMessage(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            UUID.randomUUID().toString(),
            "INTERNAL_SERVER_ERROR",
            "An unexpected error occurred",
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    public record ErrorResponse(
        String errorId,
        String errorCode,
        String message,
        Instant timestamp
    ) {}
}
```

This implementation guide covers the critical application and infrastructure components, providing concrete examples for implementing carrier integration, application services, and web controllers following the hexagonal architecture pattern.