# Shipment & Transportation Service - Detailed Architecture Guide

## Overview
This document provides detailed architectural guidance for implementing the Shipment & Transportation service using Spring Boot 3.2+, following Hexagonal Architecture and Domain-Driven Design principles.

## 1. Project Structure & Dependencies

### Maven Dependencies (pom.xml)
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>

<dependencies>
    <!-- Core Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- MongoDB -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    
    <!-- Kafka & CloudEvents -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-stream-binder-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>io.cloudevents</groupId>
        <artifactId>cloudevents-spring</artifactId>
        <version>2.5.0</version>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Scheduling -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-quartz</artifactId>
    </dependency>
    
    <!-- Monitoring -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

### Package Structure
```
src/main/java/com/example/shipment/
├── domain/                           # Pure business logic, no external dependencies
│   ├── model/                       # Aggregates, Entities, Value Objects
│   │   ├── aggregate/               # Shipment aggregate
│   │   ├── entity/                  # TrackingEvent entity
│   │   ├── valueobject/             # All value objects
│   │   └── exception/               # Domain exceptions
│   ├── service/                     # Domain services
│   └── repository/                  # Repository interfaces (ports)
├── application/                     # Application layer, orchestrates domain
│   ├── service/                     # Application services (use cases)
│   ├── port/                       # Application ports (inbound/outbound)
│   └── dto/                        # Application DTOs
├── infrastructure/                  # Implementation of ports, external concerns
│   ├── persistence/                 # MongoDB implementations
│   ├── carrier/                     # Carrier API adapters
│   ├── messaging/                   # Kafka/CloudEvents infrastructure
│   ├── outbox/                      # Transactional Outbox implementation
│   └── configuration/               # Infrastructure configuration
└── adapter/                         # External interfaces
    ├── web/                         # REST Controllers
    └── messaging/                   # Event consumers/producers
```

## 2. Domain Layer Design

### Core Aggregates

#### Shipment Aggregate Root
```java
public class Shipment {
    private ShipmentId shipmentId;
    private OrderId orderId;
    private ShipmentStatus status;
    private CarrierName carrierName;
    private TrackingNumber trackingNumber;
    private List<TrackingEvent> trackingHistory;
    private Instant createdAt;
    private Instant dispatchedAt;
    private Instant deliveredAt;
    
    // Business invariants enforced in constructor
    public Shipment(OrderId orderId, Package packageInfo, CarrierInfo carrierInfo) {
        // Validation and invariant enforcement
    }
    
    // Business methods
    public void addTrackingEvent(TrackingEvent event) {
        // Business rule: only add if timestamp is after last event
    }
    
    public void markAsDelivered(TrackingEvent deliveryEvent) {
        // Business rule: can only mark delivered if in transit
        // Raises ShipmentDelivered domain event
    }
}
```

**Aggregate Design Rationale:**
- **Size**: Contains only Shipment and its TrackingEvents to maintain transaction consistency
- **Concurrency**: One shipment per transaction reduces lock contention
- **Read/Write Patterns**: Optimized for frequent tracking updates and occasional reads

#### Value Objects Design

**ShipmentId**: UUID-based identifier with validation
```java
public record ShipmentId(UUID value) {
    public ShipmentId {
        Objects.requireNonNull(value, "Shipment ID cannot be null");
    }
    
    public static ShipmentId generate() {
        return new ShipmentId(UUID.randomUUID());
    }
}
```

**Package**: Aggregates package information from upstream events
```java
public record Package(
    PackageId packageId,
    OrderId orderId,
    Weight weight,
    Dimensions dimensions
) {
    public Package {
        // Validation: weight must be positive, dimensions must be valid
    }
}
```

### Domain Services

#### CarrierSelectionService
```java
@Service
public class CarrierSelectionService {
    private final List<ICarrierAdapter> availableCarriers;
    private final CarrierSelectionStrategy strategy;
    
    public CarrierSelectionResult selectBestCarrier(Package packageInfo) {
        // Strategy pattern for extensible selection algorithms
        return strategy.selectCarrier(packageInfo, availableCarriers);
    }
}
```

**Business Logic:**
- Compares rates and delivery times
- Applies business rules (e.g., cost vs. speed tradeoffs)
- Returns selected carrier with reasoning

## 3. Application Layer

### Use Case Services

#### ShipmentApplicationService
```java
@Service
@Transactional
public class ShipmentApplicationService {
    
    public ShipmentId createShipmentFromPackage(Package packageInfo) {
        // 1. Select best carrier
        CarrierSelectionResult selection = carrierSelectionService.selectBestCarrier(packageInfo);
        
        // 2. Create shipment with selected carrier
        CarrierInfo carrierInfo = selection.carrier().createShipment(packageInfo);
        
        // 3. Create and save aggregate
        Shipment shipment = new Shipment(packageInfo.orderId(), packageInfo, carrierInfo);
        shipmentRepository.save(shipment);
        
        // 4. Publish domain event via outbox
        publishShipmentDispatched(shipment);
        
        return shipment.shipmentId();
    }
}
```

## 4. Infrastructure Layer

### Transactional Outbox Pattern

#### OutboxEvent Document
```java
@Document(collection = "outbox_events")
public class OutboxEvent {
    @Id
    private String id;
    private String eventType;
    private String eventData;  // JSON serialized CloudEvent
    private Instant createdAt;
    private boolean published;
    private Instant publishedAt;
    private int retryCount;
}
```

#### Implementation Strategy
```java
@Service
public class OutboxService {
    
    @Transactional  // Same transaction as business operation
    public void saveEvent(CloudEvent event) {
        OutboxEvent outboxEvent = new OutboxEvent(event);
        outboxEventRepository.save(outboxEvent);
    }
    
    @Scheduled(fixedDelay = 5000)  // Every 5 seconds
    public void publishPendingEvents() {
        List<OutboxEvent> unpublishedEvents = outboxEventRepository.findUnpublished();
        
        for (OutboxEvent event : unpublishedEvents) {
            try {
                kafkaTemplate.send(event.getEventType(), event.getEventData());
                event.markAsPublished();
                outboxEventRepository.save(event);
            } catch (Exception e) {
                event.incrementRetryCount();
                // Exponential backoff logic
            }
        }
    }
}
```

### Carrier Integration

#### FedExAdapter Implementation
```java
@Component
public class FedExAdapter implements ICarrierAdapter {
    private final FedExApiClient fedExApiClient;
    private final CarrierConfigurationProperties config;
    
    @Override
    public CarrierInfo createShipment(Package packageInfo) {
        try {
            FedExShipmentRequest request = buildShipmentRequest(packageInfo);
            FedExShipmentResponse response = fedExApiClient.createShipment(request);
            
            return new CarrierInfo(
                new TrackingNumber(response.trackingNumber()),
                response.labelData(),
                CarrierName.FEDEX
            );
        } catch (FedExApiException e) {
            throw new CarrierException("Failed to create FedEx shipment", e);
        }
    }
    
    @Override
    public Optional<TrackingUpdate> getTrackingStatus(TrackingNumber trackingNumber) {
        // Similar implementation for tracking queries
    }
}
```

### Event-Driven Architecture

#### CloudEvents Integration
```java
@Component
public class CloudEventFactory {
    
    public CloudEvent createShipmentDispatchedEvent(Shipment shipment) {
        ShipmentDispatchedData data = new ShipmentDispatchedData(
            shipment.shipmentId(),
            shipment.orderId(),
            shipment.carrierName(),
            shipment.trackingNumber(),
            shipment.dispatchedAt()
        );
        
        return CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType("com.example.fulfillment.shipment.dispatched")
            .withSource(URI.create("/fulfillment/shipment-transport-service"))
            .withSubject(shipment.shipmentId().toString())
            .withTime(OffsetDateTime.now())
            .withDataContentType("application/json")
            .withData(objectMapper.writeValueAsBytes(data))
            .build();
    }
}
```

#### Kafka Consumer Configuration
```java
@Component
public class PackagePackedEventConsumer {
    
    @StreamListener("packagePackedInput")
    public void handlePackagePacked(CloudEvent event) {
        try {
            PackagePackedData data = objectMapper.readValue(
                event.getData().toBytes(), 
                PackagePackedData.class
            );
            
            Package packageInfo = mapToPackage(data.getPackage());
            shipmentApplicationService.createShipmentFromPackage(packageInfo);
            
        } catch (Exception e) {
            // Error handling and dead letter queue logic
        }
    }
}
```

### Scheduled Jobs

#### Tracking Update Job
```java
@Component
public class TrackingJobService {
    
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void updateTrackingStatus() {
        List<Shipment> inTransitShipments = shipmentRepository.findAllInTransit();
        
        for (Shipment shipment : inTransitShipments) {
            try {
                ICarrierAdapter carrier = getCarrierAdapter(shipment.carrierName());
                Optional<TrackingUpdate> update = carrier.getTrackingStatus(shipment.trackingNumber());
                
                if (update.isPresent()) {
                    shipment.addTrackingEvent(update.get().toTrackingEvent());
                    
                    if (update.get().isDelivered()) {
                        shipment.markAsDelivered(update.get().toTrackingEvent());
                        publishShipmentDelivered(shipment);
                    }
                    
                    shipmentRepository.save(shipment);
                }
                
            } catch (CarrierException e) {
                // Log and continue with next shipment
            }
        }
    }
}
```

## 5. Testing Strategy

### Domain Testing
```java
class ShipmentTest {
    @Test
    void should_add_tracking_event_when_timestamp_is_after_last_event() {
        // Given
        Shipment shipment = createShipmentWithOneEvent();
        TrackingEvent newEvent = createEventAfterLastEvent();
        
        // When
        shipment.addTrackingEvent(newEvent);
        
        // Then
        assertThat(shipment.trackingHistory()).hasSize(2);
        assertThat(shipment.trackingHistory().last()).isEqualTo(newEvent);
    }
}
```

### Integration Testing with TestContainers
```java
@SpringBootTest
@Testcontainers
class OutboxPatternIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDb = new MongoDBContainer("mongo:7.0");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
    
    @Test
    void should_publish_event_via_outbox_pattern() {
        // Test the complete flow from domain event to Kafka publication
    }
}
```

## 6. Configuration Management

### Application Configuration
```yaml
# application.yml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/shipment-transport}
      auto-index-creation: true
  
  cloud:
    stream:
      kafka:
        binder:
          brokers: ${KAFKA_BROKERS:localhost:9092}
      bindings:
        packagePackedInput:
          destination: fulfillment.warehouse.v1.events
          group: shipment-transport-service
        shipmentEventsOutput:
          destination: fulfillment.shipment.v1.events

# Carrier Configuration
carriers:
  fedex:
    api-url: ${FEDEX_API_URL}
    api-key: ${FEDEX_API_KEY}
    account-number: ${FEDEX_ACCOUNT_NUMBER}
```

## 7. Monitoring & Observability

### Actuator Endpoints
- `/actuator/health` - Service health
- `/actuator/metrics` - Performance metrics
- `/actuator/info` - Application info

### Custom Health Indicators
```java
@Component
public class CarrierHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Check connectivity to carrier APIs
        return Health.up()
            .withDetail("fedex", checkFedExHealth())
            .build();
    }
}
```

## 8. Deployment Strategy

### Docker Configuration
```dockerfile
FROM openjdk:21-jre-slim
COPY target/shipment-transport-service.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Docker Compose for Development
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      - mongodb
      - kafka
      
  mongodb:
    image: mongo:7.0
    ports:
      - "27017:27017"
      
  kafka:
    image: confluentinc/cp-kafka:7.4.0
    # Kafka configuration
```

This architectural design ensures:
- **Domain Integrity**: Pure domain logic without external dependencies
- **Flexibility**: Pluggable carrier adapters and selection strategies  
- **Reliability**: Transactional outbox pattern for guaranteed event delivery
- **Scalability**: Stateless design with MongoDB for horizontal scaling
- **Observability**: Comprehensive monitoring and health checks
- **Testability**: Clear separation of concerns enabling focused testing

The implementation follows all DDD tactical patterns and hexagonal architecture principles while leveraging Spring Boot's ecosystem effectively.