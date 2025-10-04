# Testing Implementation Guide

This guide provides comprehensive testing strategies and implementations for all layers of the application (Tasks 56-65).

## Domain Layer Testing (Tasks 56-57)

### ShipmentUnitTest (Task 56)
```java
package com.example.shipment.domain.model.aggregate;

import com.example.shipment.domain.model.entity.TrackingEvent;
import com.example.shipment.domain.model.valueobject.*;
import com.example.shipment.domain.model.exception.InvalidShipmentStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ShipmentUnitTest {
    
    private Package testPackage;
    private CarrierInfo testCarrierInfo;
    
    @BeforeEach
    void setUp() {
        // Create test package
        PackageId packageId = new PackageId(java.util.UUID.randomUUID());
        OrderId orderId = new OrderId(java.util.UUID.randomUUID());
        Weight weight = new Weight(new BigDecimal("2.5"), Weight.WeightUnit.KG);
        Dimensions dimensions = new Dimensions(
            new BigDecimal("20"),
            new BigDecimal("15"),
            new BigDecimal("10"),
            Dimensions.DimensionUnit.CM
        );
        testPackage = new Package(packageId, orderId, weight, dimensions);
        
        // Create test carrier info
        TrackingNumber trackingNumber = new TrackingNumber("784398712345");
        byte[] labelData = "test-label-data".getBytes();
        testCarrierInfo = new CarrierInfo(trackingNumber, labelData, CarrierName.FEDEX);
    }
    
    @Test
    @DisplayName("Should create shipment with initial dispatched state")
    void shouldCreateShipmentWithInitialDispatchedState() {
        // When
        Shipment shipment = new Shipment(testPackage.orderId(), testPackage, testCarrierInfo);
        
        // Then
        assertThat(shipment.getShipmentId()).isNotNull();
        assertThat(shipment.getOrderId()).isEqualTo(testPackage.orderId());
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(shipment.getCarrierName()).isEqualTo(CarrierName.FEDEX);
        assertThat(shipment.getTrackingNumber()).isEqualTo(testCarrierInfo.trackingNumber());
        assertThat(shipment.getCreatedAt()).isNotNull();
        assertThat(shipment.getDispatchedAt()).isNotNull();
        assertThat(shipment.getDeliveredAt()).isNull();
        assertThat(shipment.getTrackingHistory()).hasSize(1);
        assertThat(shipment.getTrackingHistory().get(0).getStatusDescription())
            .contains("dispatched");
    }
    
    @Test
    @DisplayName("Should add tracking event when timestamp is after last event")
    void shouldAddTrackingEventWhenTimestampIsAfterLastEvent() {
        // Given
        Shipment shipment = new Shipment(testPackage.orderId(), testPackage, testCarrierInfo);
        Location location = new Location("New York", "NY", "10001", "US");
        TrackingEvent newEvent = new TrackingEvent(
            Instant.now().plusSeconds(3600),
            "Arrived at sorting facility",
            location
        );
        
        // When
        shipment.addTrackingEvent(newEvent);
        
        // Then
        assertThat(shipment.getTrackingHistory()).hasSize(2);
        assertThat(shipment.getTrackingHistory().get(1)).isEqualTo(newEvent);
    }
    
    @Test
    @DisplayName("Should automatically mark as delivered when delivery tracking event is added")
    void shouldAutomaticallyMarkAsDeliveredWhenDeliveryTrackingEventIsAdded() {
        // Given
        Shipment shipment = new Shipment(testPackage.orderId(), testPackage, testCarrierInfo);
        TrackingEvent deliveryEvent = new TrackingEvent(
            Instant.now().plusSeconds(3600),
            "Package delivered to recipient",
            new Location("Customer City", "CA", "90210", "US")
        );
        
        // When
        shipment.addTrackingEvent(deliveryEvent);
        
        // Then
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        assertThat(shipment.getDeliveredAt()).isNotNull();
        assertThat(shipment.getTrackingHistory()).hasSize(2);
    }
}
```

### CarrierSelectionServiceTest (Task 57)
```java
package com.example.shipment.domain.service;

import com.example.shipment.domain.model.valueobject.*;
import com.example.shipment.domain.port.ICarrierAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarrierSelectionServiceTest {
    
    @Mock
    private ICarrierAdapter fedexAdapter;
    
    @Mock
    private CarrierSelectionStrategy strategy;
    
    private CarrierSelectionService carrierSelectionService;
    private Package testPackage;
    
    @BeforeEach
    void setUp() {
        List<ICarrierAdapter> adapters = List.of(fedexAdapter);
        carrierSelectionService = new CarrierSelectionService(adapters, strategy);
        
        PackageId packageId = new PackageId(java.util.UUID.randomUUID());
        OrderId orderId = new OrderId(java.util.UUID.randomUUID());
        Weight weight = new Weight(new BigDecimal("2.5"), Weight.WeightUnit.KG);
        Dimensions dimensions = new Dimensions(
            new BigDecimal("20"),
            new BigDecimal("15"),
            new BigDecimal("10"),
            Dimensions.DimensionUnit.CM
        );
        testPackage = new Package(packageId, orderId, weight, dimensions);
    }
    
    @Test
    void shouldSelectCarrierUsingStrategy() {
        // Given
        ShippingCost cost = new ShippingCost(new BigDecimal("25.99"), "USD", 3);
        CarrierSelectionResult expectedResult = new CarrierSelectionResult(
            fedexAdapter, cost, "Selected FedEx for best cost"
        );
        
        when(strategy.selectCarrier(testPackage, List.of(fedexAdapter)))
            .thenReturn(expectedResult);
        
        // When
        CarrierSelectionResult result = carrierSelectionService.selectBestCarrier(testPackage);
        
        // Then
        assertThat(result).isEqualTo(expectedResult);
        verify(strategy).selectCarrier(testPackage, List.of(fedexAdapter));
    }
}
```

## Integration Testing (Tasks 61-65)

### OutboxPatternIntegrationTest (Task 61)
```java
package com.example.shipment.infrastructure.outbox;

import com.example.shipment.infrastructure.outbox.service.OutboxService;
import com.example.shipment.infrastructure.outbox.document.OutboxEvent;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class OutboxPatternIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
        .withReuse(true);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private OutboxService outboxService;
    
    @Test
    void shouldSaveEventToOutboxWithinTransaction() {
        // Given
        CloudEvent event = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType("com.example.test.event")
            .withSource(URI.create("/test"))
            .withTime(OffsetDateTime.now())
            .withDataContentType("application/json")
            .withData("{\"test\": \"data\"}".getBytes())
            .build();
        
        // When
        outboxService.saveEvent(event);
        
        // Then
        List<OutboxEvent> unpublishedEvents = outboxService.getUnpublishedEvents(10);
        assertThat(unpublishedEvents).hasSize(1);
        
        OutboxEvent savedEvent = unpublishedEvents.get(0);
        assertThat(savedEvent.getEventType()).isEqualTo("com.example.test.event");
        assertThat(savedEvent.isPublished()).isFalse();
    }
}
```

### ShipmentRepositoryTest (Task 62)
```java
package com.example.shipment.infrastructure.persistence;

import com.example.shipment.domain.model.aggregate.Shipment;
import com.example.shipment.domain.model.valueobject.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataMongoTest
@Testcontainers
class ShipmentRepositoryTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
        .withReuse(true);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private ShipmentMongoRepository repository;
    
    @Test
    void shouldSaveAndFindShipment() {
        // Given
        Shipment shipment = createTestShipment();
        
        // When
        Shipment savedShipment = repository.save(shipment);
        Optional<Shipment> foundShipment = repository.findById(savedShipment.getShipmentId());
        
        // Then
        assertThat(foundShipment).isPresent();
        assertThat(foundShipment.get().getOrderId()).isEqualTo(shipment.getOrderId());
        assertThat(foundShipment.get().getCarrierName()).isEqualTo(shipment.getCarrierName());
    }
    
    @Test
    void shouldFindInTransitShipments() {
        // Given
        Shipment shipment1 = createTestShipment();
        Shipment shipment2 = createTestShipment();
        shipment2.markAsDelivered();
        
        repository.save(shipment1);
        repository.save(shipment2);
        
        // When
        List<Shipment> inTransitShipments = repository.findAllInTransit();
        
        // Then
        assertThat(inTransitShipments).hasSize(1);
        assertThat(inTransitShipments.get(0).getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    }
    
    private Shipment createTestShipment() {
        Package pkg = new Package(
            new PackageId(java.util.UUID.randomUUID()),
            new OrderId(java.util.UUID.randomUUID()),
            new Weight(new BigDecimal("2.5"), Weight.WeightUnit.KG),
            new Dimensions(new BigDecimal("20"), new BigDecimal("15"), new BigDecimal("10"), Dimensions.DimensionUnit.CM)
        );
        
        CarrierInfo carrierInfo = new CarrierInfo(
            new TrackingNumber("784398712345"),
            "test-label".getBytes(),
            CarrierName.FEDEX
        );
        
        return new Shipment(pkg.orderId(), pkg, carrierInfo);
    }
}
```

### TrackingJobIntegrationTest (Task 63)
```java
package com.example.shipment.infrastructure.job;

import com.example.shipment.domain.model.aggregate.Shipment;
import com.example.shipment.domain.repository.ShipmentRepository;
import com.example.shipment.domain.port.ICarrierAdapter;
import com.example.shipment.domain.model.valueobject.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
class TrackingJobIntegrationTest {
    
    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
        .withReuse(true);
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    
    @Autowired
    private TrackingJobService trackingJobService;
    
    @Autowired
    private ShipmentRepository shipmentRepository;
    
    @MockBean
    private ICarrierAdapter carrierAdapter;
    
    @Test
    void shouldUpdateTrackingForInTransitShipments() {
        // Given
        Shipment shipment = createTestShipment();
        shipmentRepository.save(shipment);
        
        when(carrierAdapter.getCarrierName()).thenReturn(CarrierName.FEDEX);
        when(carrierAdapter.getTrackingStatus(any(TrackingNumber.class)))
            .thenReturn(Optional.empty()); // No updates available
        
        // When
        trackingJobService.updateTrackingStatus();
        
        // Then
        verify(carrierAdapter).getTrackingStatus(shipment.getTrackingNumber());
    }
    
    private Shipment createTestShipment() {
        Package pkg = new Package(
            new PackageId(java.util.UUID.randomUUID()),
            new OrderId(java.util.UUID.randomUUID()),
            new Weight(new BigDecimal("2.5"), Weight.WeightUnit.KG),
            new Dimensions(new BigDecimal("20"), new BigDecimal("15"), new BigDecimal("10"), Dimensions.DimensionUnit.CM)
        );
        
        CarrierInfo carrierInfo = new CarrierInfo(
            new TrackingNumber("784398712345"),
            "test-label".getBytes(),
            CarrierName.FEDEX
        );
        
        return new Shipment(pkg.orderId(), pkg, carrierInfo);
    }
}
```

## Test Configuration (Tasks 64-65)

### application-test.yml (Task 64)
```yaml
# Test-specific configuration
spring:
  data:
    mongodb:
      # Will be overridden by TestContainers
      uri: mongodb://localhost:27017/shipment_transport_test
  
  kafka:
    # Disable Kafka for most tests
    bootstrap-servers: ${spring.embedded.kafka.brokers:localhost:9092}
  
  cloud:
    stream:
      # Disable cloud stream bindings in tests
      bindings:
        packagePackedInput:
          destination: test-topic
        shipmentEventsOutput:
          destination: test-topic
  
  jpa:
    show-sql: true
  
  logging:
    level:
      com.example.shipment: DEBUG
      org.springframework.data.mongodb: DEBUG
      
# Carrier configuration for testing
carriers:
  fedex:
    api-url: http://localhost:8089
    api-key: test-key
    account-number: test-account
  
# Disable scheduling in tests
scheduling:
  enabled: false

# Test-specific actuator settings
management:
  endpoints:
    web:
      exposure:
        include: health,info
```

### TestContainers Configuration (Task 65)
```java
package com.example.shipment.infrastructure.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfiguration {
    
    public static final MongoDBContainer MONGODB_CONTAINER = new MongoDBContainer("mongo:7.0")
        .withReuse(true);
    
    public static final KafkaContainer KAFKA_CONTAINER = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
        .withReuse(true);
    
    static {
        MONGODB_CONTAINER.start();
        KAFKA_CONTAINER.start();
    }
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MongoDB configuration
        registry.add("spring.data.mongodb.uri", MONGODB_CONTAINER::getReplicaSetUrl);
        
        // Kafka configuration
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.cloud.stream.kafka.binder.brokers", KAFKA_CONTAINER::getBootstrapServers);
    }
    
    @Bean
    public MongoDBContainer mongoDBContainer() {
        return MONGODB_CONTAINER;
    }
    
    @Bean
    public KafkaContainer kafkaContainer() {
        return KAFKA_CONTAINER;
    }
}
```

### Base Integration Test Class
```java
package com.example.shipment.infrastructure.test;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@ContextConfiguration(classes = TestContainersConfiguration.class)
@Tag("integration")
public abstract class BaseIntegrationTest {
    // Base class for all integration tests
    // Provides common TestContainers setup and Spring configuration
}
```

### Test Utilities
```java
package com.example.shipment.infrastructure.test;

import com.example.shipment.domain.model.valueobject.*;
import com.example.shipment.domain.model.aggregate.Shipment;
import java.math.BigDecimal;
import java.util.UUID;

public class TestDataFactory {
    
    public static Package createTestPackage() {
        return new Package(
            new PackageId(UUID.randomUUID()),
            new OrderId(UUID.randomUUID()),
            new Weight(new BigDecimal("2.5"), Weight.WeightUnit.KG),
            new Dimensions(
                new BigDecimal("20"), 
                new BigDecimal("15"), 
                new BigDecimal("10"), 
                Dimensions.DimensionUnit.CM
            )
        );
    }
    
    public static CarrierInfo createTestCarrierInfo() {
        return new CarrierInfo(
            new TrackingNumber("784398712345"),
            "test-label-data".getBytes(),
            CarrierName.FEDEX
        );
    }
    
    public static Shipment createTestShipment() {
        Package pkg = createTestPackage();
        CarrierInfo carrierInfo = createTestCarrierInfo();
        return new Shipment(pkg.orderId(), pkg, carrierInfo);
    }
}
```

This testing guide provides comprehensive coverage for all layers:
- **Unit Tests**: Focus on domain logic and individual components
- **Integration Tests**: Verify component interactions and database operations
- **Contract Tests**: Ensure external API integrations work correctly
- **TestContainers**: Provide real database and messaging infrastructure for tests

The testing strategy follows the test pyramid principle with fast unit tests at the base, fewer integration tests in the middle, and minimal end-to-end tests at the top.