# Persistence, Events & Jobs Implementation Guide

This guide covers MongoDB persistence, transactional outbox pattern, CloudEvents integration, scheduled jobs, and configuration (Tasks 35-55).

## MongoDB Persistence Layer (Tasks 35-38)

### ShipmentMongoRepository (Task 35)
```java
package com.example.shipment.infrastructure.persistence;

import com.example.shipment.domain.model.aggregate.Shipment;
import com.example.shipment.domain.model.valueobject.*;
import com.example.shipment.domain.repository.ShipmentRepository;
import com.example.shipment.infrastructure.persistence.document.ShipmentDocument;
import com.example.shipment.infrastructure.persistence.mapper.ShipmentDocumentMapper;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class ShipmentMongoRepository implements ShipmentRepository {
    private final MongoTemplate mongoTemplate;
    private final ShipmentDocumentMapper mapper;
    
    public ShipmentMongoRepository(MongoTemplate mongoTemplate, ShipmentDocumentMapper mapper) {
        this.mongoTemplate = mongoTemplate;
        this.mapper = mapper;
    }
    
    @Override
    public Shipment save(Shipment shipment) {
        ShipmentDocument document = mapper.toDocument(shipment);
        ShipmentDocument savedDocument = mongoTemplate.save(document);
        return mapper.toDomain(savedDocument);
    }
    
    @Override
    public Optional<Shipment> findById(ShipmentId shipmentId) {
        ShipmentDocument document = mongoTemplate.findById(
            shipmentId.value().toString(), 
            ShipmentDocument.class
        );
        return document != null ? Optional.of(mapper.toDomain(document)) : Optional.empty();
    }
    
    @Override
    public Optional<Shipment> findByOrderId(OrderId orderId) {
        Query query = new Query(Criteria.where("orderId").is(orderId.value().toString()));
        ShipmentDocument document = mongoTemplate.findOne(query, ShipmentDocument.class);
        return document != null ? Optional.of(mapper.toDomain(document)) : Optional.empty();
    }
    
    @Override
    public List<Shipment> findAllInTransit() {
        Query query = new Query(Criteria.where("status").is("IN_TRANSIT"));
        List<ShipmentDocument> documents = mongoTemplate.find(query, ShipmentDocument.class);
        return documents.stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public List<Shipment> findAll() {
        List<ShipmentDocument> documents = mongoTemplate.findAll(ShipmentDocument.class);
        return documents.stream()
            .map(mapper::toDomain)
            .toList();
    }
    
    @Override
    public void delete(ShipmentId shipmentId) {
        Query query = new Query(Criteria.where("id").is(shipmentId.value().toString()));
        mongoTemplate.remove(query, ShipmentDocument.class);
    }
    
    @Override
    public boolean existsById(ShipmentId shipmentId) {
        Query query = new Query(Criteria.where("id").is(shipmentId.value().toString()));
        return mongoTemplate.exists(query, ShipmentDocument.class);
    }
}
```

### ShipmentDocument (Task 36)
```java
package com.example.shipment.infrastructure.persistence.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.Instant;
import java.util.List;

@Document(collection = "shipments")
public class ShipmentDocument {
    @Id
    private String id;
    
    @Field("order_id")
    @Indexed(unique = true)
    private String orderId;
    
    @Field("status")
    @Indexed
    private String status;
    
    @Field("carrier_name")
    private String carrierName;
    
    @Field("tracking_number")
    @Indexed
    private String trackingNumber;
    
    @Field("tracking_history")
    private List<TrackingEventDocument> trackingHistory;
    
    @Field("created_at")
    private Instant createdAt;
    
    @Field("dispatched_at")
    private Instant dispatchedAt;
    
    @Field("delivered_at")
    private Instant deliveredAt;
    
    // Default constructor for MongoDB
    public ShipmentDocument() {}
    
    // Full constructor
    public ShipmentDocument(String id, String orderId, String status, String carrierName,
                           String trackingNumber, List<TrackingEventDocument> trackingHistory,
                           Instant createdAt, Instant dispatchedAt, Instant deliveredAt) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.carrierName = carrierName;
        this.trackingNumber = trackingNumber;
        this.trackingHistory = trackingHistory;
        this.createdAt = createdAt;
        this.dispatchedAt = dispatchedAt;
        this.deliveredAt = deliveredAt;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getCarrierName() { return carrierName; }
    public void setCarrierName(String carrierName) { this.carrierName = carrierName; }
    
    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    
    public List<TrackingEventDocument> getTrackingHistory() { return trackingHistory; }
    public void setTrackingHistory(List<TrackingEventDocument> trackingHistory) { 
        this.trackingHistory = trackingHistory; 
    }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getDispatchedAt() { return dispatchedAt; }
    public void setDispatchedAt(Instant dispatchedAt) { this.dispatchedAt = dispatchedAt; }
    
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
}
```

### TrackingEventDocument (Task 37)
```java
package com.example.shipment.infrastructure.persistence.document;

import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

public class TrackingEventDocument {
    @Field("timestamp")
    private Instant timestamp;
    
    @Field("status_description")
    private String statusDescription;
    
    @Field("location")
    private LocationDocument location;
    
    // Default constructor
    public TrackingEventDocument() {}
    
    // Constructor
    public TrackingEventDocument(Instant timestamp, String statusDescription, LocationDocument location) {
        this.timestamp = timestamp;
        this.statusDescription = statusDescription;
        this.location = location;
    }
    
    // Getters and setters
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    
    public String getStatusDescription() { return statusDescription; }
    public void setStatusDescription(String statusDescription) { this.statusDescription = statusDescription; }
    
    public LocationDocument getLocation() { return location; }
    public void setLocation(LocationDocument location) { this.location = location; }
    
    public static class LocationDocument {
        @Field("city")
        private String city;
        
        @Field("state_or_region") 
        private String stateOrRegion;
        
        @Field("postal_code")
        private String postalCode;
        
        @Field("country_code")
        private String countryCode;
        
        // Constructors, getters, setters
        public LocationDocument() {}
        
        public LocationDocument(String city, String stateOrRegion, String postalCode, String countryCode) {
            this.city = city;
            this.stateOrRegion = stateOrRegion;
            this.postalCode = postalCode;
            this.countryCode = countryCode;
        }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        
        public String getStateOrRegion() { return stateOrRegion; }
        public void setStateOrRegion(String stateOrRegion) { this.stateOrRegion = stateOrRegion; }
        
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    }
}
```

### MongoDB Configuration (Task 38)
```java
package com.example.shipment.infrastructure.configuration;

import com.mongodb.client.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableMongoRepositories(basePackages = "com.example.shipment.infrastructure.persistence")
public class MongoConfiguration extends AbstractMongoClientConfiguration {
    
    @Override
    protected String getDatabaseName() {
        return "shipment_transport";
    }
    
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }
    
    @Bean
    @Override
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(java.util.Collections.emptyList());
    }
    
    @Bean
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, getDatabaseName());
    }
}
```

## Event Integration (Tasks 47-49)

### PackagePackedEventConsumer (Task 47)
```java
package com.example.shipment.adapter.messaging.consumer;

import com.example.shipment.application.service.ShipmentApplicationService;
import com.example.shipment.infrastructure.messaging.event.PackagePackedData;
import com.example.shipment.domain.model.valueobject.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.stereotype.Component;

@Component
public class PackagePackedEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(PackagePackedEventConsumer.class);
    
    private final ShipmentApplicationService shipmentApplicationService;
    private final ObjectMapper objectMapper;
    
    public PackagePackedEventConsumer(
            ShipmentApplicationService shipmentApplicationService,
            ObjectMapper objectMapper) {
        this.shipmentApplicationService = shipmentApplicationService;
        this.objectMapper = objectMapper;
    }
    
    @StreamListener("packagePackedInput")
    public void handlePackagePacked(CloudEvent event) {
        logger.info("Received PackagePacked event: {}", event.getId());
        
        try {
            PackagePackedData eventData = objectMapper.readValue(
                event.getData().toBytes(),
                PackagePackedData.class
            );
            
            Package packageInfo = mapToPackage(eventData.getPackageData());
            shipmentApplicationService.createShipmentFromPackage(packageInfo);
            
            logger.info("Successfully processed PackagePacked event for order: {}", 
                packageInfo.orderId());
                
        } catch (Exception e) {
            logger.error("Failed to process PackagePacked event: {}", event.getId(), e);
            // In a real implementation, you might:
            // 1. Send to dead letter queue
            // 2. Implement retry logic
            // 3. Alert monitoring systems
            throw new RuntimeException("Event processing failed", e);
        }
    }
    
    private Package mapToPackage(PackagePackedData.PackageData data) {
        PackageId packageId = new PackageId(data.getPackageId());
        OrderId orderId = new OrderId(data.getOrderId());
        
        Weight weight = new Weight(
            data.getWeight().getValue(),
            Weight.WeightUnit.valueOf(data.getWeight().getUnit().toUpperCase())
        );
        
        Dimensions dimensions = new Dimensions(
            data.getDimensions().getLength(),
            data.getDimensions().getWidth(),
            data.getDimensions().getHeight(),
            Dimensions.DimensionUnit.valueOf(data.getDimensions().getUnit().toUpperCase())
        );
        
        return new Package(packageId, orderId, weight, dimensions);
    }
}
```

### ShipmentEventPublisher (Task 48)
```java
package com.paklog.shipment.infrastructure.events;

import com.paklog.shipment.application.port.ShipmentEventPublisher;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.infrastructure.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ShipmentEventPublisherImpl implements ShipmentEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ShipmentEventPublisherImpl.class);

    private final OutboxService outboxService;
    private final CloudEventSerializer cloudEventSerializer;
    private final ShipmentEventProperties shipmentEventProperties;

    public ShipmentEventPublisherImpl(OutboxService outboxService,
                                      ObjectMapper objectMapper,
                                      ShipmentEventProperties shipmentEventProperties) {
        this.outboxService = outboxService;
        this.cloudEventSerializer = new CloudEventSerializer(objectMapper);
        this.shipmentEventProperties = shipmentEventProperties;
    }

    @Override
    public void shipmentDispatched(Shipment shipment) {
        String payload = createDispatchedPayload(shipment);
        persist(shipment, shipmentEventProperties.getDispatched().getType(), payload);
    }

    @Override
    public void shipmentDelivered(Shipment shipment) {
        String payload = createDeliveredPayload(shipment);
        persist(shipment, shipmentEventProperties.getDelivered().getType(), payload);
    }

    private void persist(Shipment shipment, String type, String payload) {
        String destination = type.equals(shipmentEventProperties.getDispatched().getType())
                ? shipmentEventProperties.getDispatched().getTopic()
                : shipmentEventProperties.getDelivered().getTopic();

        String serialized = cloudEventSerializer.serialize(
                shipment.getId().toString(),
                shipment.getId().toString(),
                "Shipment",
                type,
                payload,
                null
        );

        outboxService.save(new SimpleDomainEvent(
                shipment.getId().toString(),
                "Shipment",
                type,
                destination,
                serialized
        ));
        logger.debug("Queued {} event for shipment {}", type, shipment.getId());
    }

    private String createDispatchedPayload(Shipment shipment) {
        return String.format("{\"shipment_id\":\"%s\",\"order_id\":\"%s\",\"carrier_name\":\"%s\",\"tracking_number\":\"%s\",\"dispatched_at\":\"%s\"}",
                shipment.getId(),
                shipment.getOrderId().getValue(),
                shipment.getCarrierName().name(),
                shipment.getTrackingNumber().getValue(),
                shipment.getDispatchedAt());
    }

    private String createDeliveredPayload(Shipment shipment) {
        return String.format("{\"shipment_id\":\"%s\",\"order_id\":\"%s\",\"carrier_name\":\"%s\",\"tracking_number\":\"%s\",\"delivered_at\":\"%s\"}",
                shipment.getId(),
                shipment.getOrderId().getValue(),
                shipment.getCarrierName().name(),
                shipment.getTrackingNumber().getValue(),
                shipment.getDeliveredAt());
    }
}
```

### Spring Cloud Stream Configuration (Task 49)
```java
package com.example.shipment.infrastructure.configuration;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBinding(Sink.class)
public class StreamConfiguration {
    // This configuration enables Spring Cloud Stream bindings
    // The actual binding configuration is in application.yml
}
```

## Scheduled Jobs (Tasks 50-52)

### TrackingJobService (Task 50)
```java
package com.example.shipment.infrastructure.job;

import com.example.shipment.application.service.ShipmentApplicationService;
import com.example.shipment.domain.model.aggregate.Shipment;
import com.example.shipment.domain.model.entity.TrackingEvent;
import com.example.shipment.domain.model.valueobject.TrackingUpdate;
import com.example.shipment.domain.port.ICarrierAdapter;
import com.example.shipment.domain.repository.ShipmentRepository;
import com.example.shipment.domain.model.exception.CarrierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TrackingJobService {
    private static final Logger logger = LoggerFactory.getLogger(TrackingJobService.class);
    
    private final ShipmentRepository shipmentRepository;
    private final ShipmentApplicationService shipmentApplicationService;
    private final Map<String, ICarrierAdapter> carrierAdapters;
    
    public TrackingJobService(
            ShipmentRepository shipmentRepository,
            ShipmentApplicationService shipmentApplicationService,
            List<ICarrierAdapter> carrierAdapterList) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentApplicationService = shipmentApplicationService;
        this.carrierAdapters = carrierAdapterList.stream()
            .collect(Collectors.toMap(
                adapter -> adapter.getCarrierName().name(),
                Function.identity()
            ));
    }
    
    @Scheduled(fixedRate = 3600000) // Every hour
    public void updateTrackingStatus() {
        logger.info("Starting tracking update job");
        
        List<Shipment> inTransitShipments = shipmentRepository.findAllInTransit();
        logger.info("Found {} shipments in transit", inTransitShipments.size());
        
        int successCount = 0;
        int errorCount = 0;
        
        for (Shipment shipment : inTransitShipments) {
            try {
                boolean updated = updateShipmentTracking(shipment);
                if (updated) {
                    successCount++;
                }
            } catch (Exception e) {
                errorCount++;
                logger.error("Failed to update tracking for shipment: {}", 
                    shipment.getShipmentId(), e);
            }
        }
        
        logger.info("Tracking update job completed. Success: {}, Errors: {}", 
            successCount, errorCount);
    }
    
    private boolean updateShipmentTracking(Shipment shipment) {
        ICarrierAdapter carrier = carrierAdapters.get(shipment.getCarrierName().name());
        if (carrier == null) {
            logger.warn("No carrier adapter found for: {}", shipment.getCarrierName());
            return false;
        }
        
        try {
            Optional<TrackingUpdate> update = carrier.getTrackingStatus(shipment.getTrackingNumber());
            
            if (update.isPresent()) {
                TrackingUpdate trackingUpdate = update.get();
                List<TrackingEvent> newEvents = trackingUpdate.newEvents();
                
                if (!newEvents.isEmpty()) {
                    logger.debug("Found {} new tracking events for shipment: {}", 
                        newEvents.size(), shipment.getShipmentId());
                    
                    shipmentApplicationService.updateShipmentTracking(
                        shipment.getShipmentId(), 
                        newEvents
                    );
                    return true;
                }
            }
            
        } catch (CarrierException e) {
            logger.warn("Carrier error updating tracking for shipment {}: {}", 
                shipment.getShipmentId(), e.getMessage());
        }
        
        return false;
    }
}
```

### TrackingJobConfiguration (Task 51)
```java
package com.example.shipment.infrastructure.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
public class TrackingJobConfiguration implements SchedulingConfigurer {
    
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // Configure thread pool for scheduled tasks
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(2));
    }
}
```

### ShipmentTrackingUpdater (Task 52)
```java
package com.example.shipment.infrastructure.job;

import com.example.shipment.domain.model.aggregate.Shipment;
import com.example.shipment.domain.model.entity.TrackingEvent;
import com.example.shipment.domain.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ShipmentTrackingUpdater {
    private static final Logger logger = LoggerFactory.getLogger(ShipmentTrackingUpdater.class);
    
    private final ShipmentRepository shipmentRepository;
    
    public ShipmentTrackingUpdater(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }
    
    @Transactional
    public void updateShipmentWithNewEvents(Shipment shipment, List<TrackingEvent> newEvents) {
        logger.debug("Updating shipment {} with {} new events", 
            shipment.getShipmentId(), newEvents.size());
        
        boolean wasDelivered = shipment.isDelivered();
        
        for (TrackingEvent event : newEvents) {
            shipment.addTrackingEvent(event);
        }
        
        shipmentRepository.save(shipment);
        
        // Log status change
        if (!wasDelivered && shipment.isDelivered()) {
            logger.info("Shipment {} marked as delivered", shipment.getShipmentId());
        }
    }
}
```

## Validation & Exception Handling (Tasks 53-55)

### Custom Validation Annotations (Task 53)
```java
package com.example.shipment.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ValidUUID.UUIDValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUUID {
    String message() default "Invalid UUID format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    class UUIDValidator implements ConstraintValidator<ValidUUID, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null) {
                return true; // Let @NotNull handle null validation
            }
            
            try {
                java.util.UUID.fromString(value);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
}

// ValidTrackingNumber annotation
@Documented
@Constraint(validatedBy = ValidTrackingNumber.TrackingNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTrackingNumber {
    String message() default "Invalid tracking number format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    class TrackingNumberValidator implements ConstraintValidator<ValidTrackingNumber, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
            
            // Basic format validation - can be enhanced per carrier
            return value.trim().length() >= 8 && value.trim().length() <= 20;
        }
    }
}
```

### ValidationException (Task 54)
```java
package com.example.shipment.domain.model.exception;

import java.util.List;
import java.util.Map;

public class ValidationException extends RuntimeException {
    private final Map<String, List<String>> fieldErrors;
    
    public ValidationException(String message) {
        super(message);
        this.fieldErrors = Map.of();
    }
    
    public ValidationException(String message, Map<String, List<String>> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }
    
    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }
    
    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
}
```

### ExceptionTranslationService (Task 55)
```java
package com.example.shipment.infrastructure.exception;

import com.example.shipment.domain.model.exception.*;
import com.example.shipment.application.exception.ShipmentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ExceptionTranslationService {
    
    public HttpStatus mapToHttpStatus(Exception exception) {
        return switch (exception) {
            case ShipmentNotFoundException ex -> HttpStatus.NOT_FOUND;
            case ValidationException ex -> HttpStatus.BAD_REQUEST;
            case InvalidShipmentStateException ex -> HttpStatus.BAD_REQUEST;
            case CarrierException ex -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
    
    public String mapToErrorCode(Exception exception) {
        return switch (exception) {
            case ShipmentNotFoundException ex -> "SHIPMENT_NOT_FOUND";
            case ValidationException ex -> "VALIDATION_ERROR";
            case InvalidShipmentStateException ex -> "INVALID_SHIPMENT_STATE";
            case CarrierException ex -> "CARRIER_ERROR";
            default -> "INTERNAL_SERVER_ERROR";
        };
    }
    
    public String mapToUserFriendlyMessage(Exception exception) {
        return switch (exception) {
            case ShipmentNotFoundException ex -> "The requested shipment could not be found";
            case ValidationException ex -> "The provided data is invalid";
            case InvalidShipmentStateException ex -> "The operation cannot be performed in the current shipment state";
            case CarrierException ex -> "There was a problem communicating with the shipping carrier";
            default -> "An unexpected error occurred";
        };
    }
}
```

This guide provides complete implementations for persistence, event handling, scheduled jobs, and validation components, ensuring reliable data storage, event-driven communication, and robust error handling throughout the system.
