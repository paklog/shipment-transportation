# Shipment & Transportation Service

Carrier integration and shipment tracking service with DDD, hexagonal architecture, CloudEvents, and transactional outbox pattern.

## Overview

The Shipment & Transportation Service manages shipment creation, carrier integration, and tracking within the Paklog fulfillment platform. This bounded context handles carrier selection, load tendering, shipping label generation, and real-time tracking updates. It integrates with external carrier APIs (FedEx, UPS, etc.) and provides shipment visibility across the fulfillment lifecycle.

## Domain-Driven Design

### Bounded Context
**Shipment & Transportation Management** - Manages shipments, carrier integrations, load tendering, and tracking throughout the delivery journey.

### Core Domain Model

#### Aggregates
- **Shipment** - Root aggregate representing a shipment with tracking and carrier information
- **Load** - Aggregate representing a tender/booking with a carrier

#### Entities
- **TrackingEvent** - Individual tracking update in shipment journey
- **Tender** - Carrier tender/booking request
- **Pickup** - Pickup schedule information

#### Value Objects
- **ShipmentId** - Unique shipment identifier
- **LoadId** - Unique load identifier
- **OrderId** - Reference to fulfillment order
- **TrackingNumber** - Carrier tracking number
- **CarrierName** - Carrier identifier
- **CarrierInfo** - Carrier details and capabilities
- **Package** - Package dimensions and weight
- **Location** - Geographic location information
- **ShippingCost** - Cost and currency
- **TrackingUpdate** - Tracking status update details
- **ShipmentStatus** - Shipment lifecycle status
- **LoadStatus** - Load processing status
- **TenderStatus** - Tender response status

#### Domain Events
- **PackagePackedEvent** - Package packed and ready for shipment (consumed)
- **LoadCreatedEvent** - Load tender created
- **ShipmentDispatchedEvent** - Shipment dispatched with carrier
- **ShipmentDeliveredEvent** - Shipment delivered to customer

#### Domain Services
- **CarrierSelectionService** - Selects optimal carrier based on strategy
- **DefaultCarrierSelectionStrategy** - Default carrier selection logic

### Ubiquitous Language
- **Shipment**: Package or set of packages being transported
- **Load**: Tender or booking with a carrier
- **Carrier**: Third-party logistics provider (FedEx, UPS, etc.)
- **Tender**: Request to carrier for shipment service
- **Tracking Number**: Carrier-provided shipment identifier
- **Tracking Event**: Status update in shipment journey
- **Dispatch**: Handover of shipment to carrier
- **BOL (Bill of Lading)**: Shipping document
- **Manifest**: List of shipments in a load

## Architecture & Patterns

### Hexagonal Architecture (Ports and Adapters)

```
src/main/java/com/paklog/shipment/
├── domain/                           # Core business logic
│   ├── Shipment.java                # Main aggregate root
│   ├── Load.java                    # Load aggregate
│   ├── TrackingEvent.java           # Entity
│   ├── Package.java                 # Value object
│   ├── CarrierName.java             # Value object
│   ├── repository/                  # Repository interfaces (ports)
│   ├── services/                    # Domain services
│   └── events/                      # Domain events
├── application/                      # Use cases & orchestration
│   ├── service/                     # Application services
│   ├── port/                        # Application ports
│   └── dto/                         # Application DTOs
└── infrastructure/                   # External adapters
    ├── persistence/                 # MongoDB repositories
    ├── carrier/                     # Carrier API adapters
    ├── messaging/                   # Kafka consumers/publishers
    ├── outbox/                      # Outbox scheduler
    ├── job/                         # Scheduled tracking jobs
    └── configuration/               # Configuration
```

### Design Patterns & Principles
- **Hexagonal Architecture** - Clean separation of domain and infrastructure
- **Domain-Driven Design** - Rich domain model with business invariants
- **Event-Driven Architecture** - Integration via domain events
- **Transactional Outbox Pattern** - Guaranteed event delivery
- **Adapter Pattern** - Unified interface for multiple carriers
- **Strategy Pattern** - Pluggable carrier selection strategies
- **Repository Pattern** - Data access abstraction
- **SOLID Principles** - Maintainable and extensible code

## Technology Stack

### Core Framework
- **Java 21** - Programming language
- **Spring Boot 3.2+** - Application framework
- **Maven** - Build and dependency management

### Data & Persistence
- **MongoDB** - Document database for aggregates
- **Spring Data MongoDB** - Data access layer

### Messaging & Events
- **Apache Kafka** - Event streaming platform
- **Spring Kafka** - Kafka integration
- **CloudEvents** - Standardized event format

### External Integrations
- **FedEx API** - FedEx carrier integration
- **UPS API** - UPS carrier integration
- **WireMock** - API mocking for testing

### API & Documentation
- **Spring Web MVC** - REST API framework
- **SpringDoc OpenAPI** - API documentation
- **Bean Validation** - Input validation

### Observability
- **Spring Boot Actuator** - Health checks and metrics
- **Micrometer** - Metrics collection
- **OpenTelemetry** - Distributed tracing
- **Prometheus** - Metrics aggregation
- **Loki** - Log aggregation

### Testing
- **JUnit 5** - Unit testing framework
- **Testcontainers** - Integration testing
- **WireMock** - Carrier API mocking
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Local development environment

## Standards Applied

### Architectural Standards
- ✅ Hexagonal Architecture (Ports and Adapters)
- ✅ Domain-Driven Design tactical patterns
- ✅ Event-Driven Architecture
- ✅ Microservices architecture
- ✅ RESTful API design
- ✅ Anti-Corruption Layer for external APIs

### Code Quality Standards
- ✅ SOLID principles
- ✅ Clean Code practices
- ✅ Comprehensive unit and integration testing
- ✅ Domain-driven design patterns
- ✅ Immutable value objects
- ✅ Rich domain models with business logic

### Event & Integration Standards
- ✅ CloudEvents specification v1.0
- ✅ Transactional Outbox Pattern
- ✅ At-least-once delivery semantics
- ✅ Event versioning strategy
- ✅ Idempotent event consumers

### Observability Standards
- ✅ Structured logging (JSON)
- ✅ Distributed tracing (OpenTelemetry)
- ✅ Health check endpoints
- ✅ Prometheus metrics
- ✅ Correlation ID propagation
- ✅ Custom business metrics

## Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Local Development

1. **Clone the repository**
   ```bash
   git clone https://github.com/paklog/shipment-transportation.git
   cd shipment-transportation
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d mongodb kafka
   ```

3. **Build and run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify the service is running**
   ```bash
   curl http://localhost:8080/api/management/health
   ```

### Using Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down
```

## API Documentation

Once running, access the interactive API documentation:
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api/api-docs

### Key Endpoints

- `GET /api/v1/shipments/{shipmentId}/tracking` - Get shipment tracking details
- `GET /api/management/health` - Health check with carrier status
- `GET /api/management/metrics` - Application metrics
- `GET /api/management/prometheus` - Prometheus metrics

## Testing

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run tests with coverage
mvn clean verify jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Configuration

Key configuration properties:

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/shipment_transport}
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS:localhost:9092}

carriers:
  fedex:
    api-url: ${FEDEX_API_URL}
    api-key: ${FEDEX_API_KEY}
    account-number: ${FEDEX_ACCOUNT_NUMBER}
  ups:
    api-url: ${UPS_API_URL}
    api-key: ${UPS_API_KEY}
    account-number: ${UPS_ACCOUNT_NUMBER}

tracking:
  job:
    enabled: ${TRACKING_JOB_ENABLED:true}
    interval: ${TRACKING_JOB_INTERVAL:3600000}
```

## Event Integration

### Consumed Events
- `com.paklog.warehouse.package.packed` - From Warehouse Operations

### Published Events
- `com.paklog.shipment.dispatched.v1`
- `com.paklog.shipment.delivered.v1`
- `com.paklog.shipment.load.created.v1`

### Event Format
All events follow the CloudEvents specification v1.0 and are published via the transactional outbox pattern.

## Carrier Integration

### Supported Carriers
- **FedEx** - Full integration with rate shopping and tracking
- **UPS** - Full integration with rate shopping and tracking

### Adding New Carriers

1. Create carrier adapter implementing `ICarrierAdapter`
2. Add carrier configuration
3. Register adapter in Spring context
4. Add integration tests with WireMock

## Background Jobs

### Tracking Update Job
- **Purpose**: Polls carrier APIs for shipment updates
- **Frequency**: Configurable (default: 1 hour)
- **Metrics**: `tracking.jobs.succeeded`, `tracking.jobs.failed`

## Monitoring

- **Health**: http://localhost:8080/api/management/health
  - MongoDB connectivity
  - Kafka connectivity
  - Carrier API availability
  - Outbox event status
- **Metrics**: http://localhost:8080/api/management/metrics
- **Prometheus**: http://localhost:8080/api/management/prometheus
- **Info**: http://localhost:8080/api/management/info

### Custom Metrics
- `shipments.created` - Shipments created
- `loads.tendered` - Load tenders sent
- `loads.booked` - Load bookings confirmed
- `carrier.api.calls` - Carrier API invocations
- `carrier.api.latency` - Carrier API response times
- `tracking.jobs.succeeded` - Successful tracking updates
- `tracking.jobs.failed` - Failed tracking updates

## Contributing

1. Follow hexagonal architecture principles
2. Implement domain logic in domain layer
3. Use carrier adapters for external integrations
4. Maintain aggregate consistency boundaries
5. Use transactional outbox for event publishing
6. Write comprehensive tests including contract tests
7. Document domain concepts using ubiquitous language
8. Follow existing code style and conventions

## License

Copyright © 2024 Paklog. All rights reserved.
