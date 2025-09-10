# Shipment & Transportation Service

A Spring Boot microservice implementing Domain-Driven Design and Hexagonal Architecture for managing shipments and carrier integrations in the fulfillment domain.

## 🏗️ Architecture Overview

This service implements the **Shipment & Transportation Bounded Context** with the following architectural patterns:

- **Hexagonal Architecture** (Ports and Adapters)
- **Domain-Driven Design** tactical patterns
- **Event-Driven Architecture** with CloudEvents
- **Transactional Outbox Pattern** for reliable event publishing
- **CQRS-like separation** between commands and queries

### Core Business Capabilities

- **Carrier Integration**: Select optimal carriers and generate shipping labels
- **Shipment Tracking**: Monitor packages throughout their journey
- **Event Publishing**: Notify other services of shipment lifecycle events

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- MongoDB 7.0+
- Apache Kafka 2.8+

### Local Development Setup

1. **Clone the repository**
```bash
git clone <repository-url>
cd shipment-transportation
```

2. **Start infrastructure services**
```bash
docker-compose up -d mongodb kafka wiremock
```

3. **Run the application**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

4. **Verify the service is running**
```bash
curl http://localhost:8080/api/management/health
```

### Using Docker Compose (Recommended for Development)

```bash
# Start all services including the application
docker-compose up

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down
```

## 📋 API Documentation

### Interactive API Explorer

Once the service is running, visit:
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api/api-docs

### Core Endpoints

#### Get Shipment Tracking
```http
GET /api/v1/shipments/{shipmentId}/tracking
```

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/v1/shipments/550e8400-e29b-41d4-a716-446655440000/tracking" \
  -H "Accept: application/json"
```

**Example Response:**
```json
{
  "shipment_id": "550e8400-e29b-41d4-a716-446655440000",
  "order_id": "987fcdeb-51a2-43d1-9c4f-123456789abc",
  "status": "in_transit",
  "carrier_name": "FedEx",
  "tracking_number": "784398712345",
  "created_at": "2023-12-01T10:30:00Z",
  "dispatched_at": "2023-12-01T11:00:00Z",
  "delivered_at": null,
  "tracking_history": [
    {
      "timestamp": "2023-12-01T11:00:00Z",
      "status_description": "Package dispatched from fulfillment center",
      "location": null
    },
    {
      "timestamp": "2023-12-01T15:30:00Z",
      "status_description": "Arrived at sorting facility",
      "location": {
        "city": "New York",
        "state_or_region": "NY",
        "postal_code": "10001",
        "country_code": "US"
      }
    }
  ]
}
```

#### Health Check
```http
GET /api/management/health
```

**Example Response:**
```json
{
  "status": "UP",
  "components": {
    "mongo": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    },
    "kafka": {
      "status": "UP"
    },
    "carriers": {
      "status": "UP",
      "details": {
        "FedEx": "UP",
        "UPS": "UP"
      }
    },
    "outbox": {
      "status": "UP",
      "details": {
        "unpublished_events": 0
      }
    }
  }
}
```

## 🔄 Event Integration

This service participates in the event-driven architecture by:

### Consuming Events

**PackagePacked Event** (from Warehouse Service):
```json
{
  "specversion": "1.0",
  "type": "com.example.fulfillment.warehouse.package.packed",
  "source": "/fulfillment/warehouse-service",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "time": "2023-12-01T10:30:00Z",
  "datacontenttype": "application/json",
  "data": {
    "package": {
      "package_id": "123e4567-e89b-12d3-a456-426614174000",
      "order_id": "987fcdeb-51a2-43d1-9c4f-123456789abc",
      "weight": {
        "value": 2.5,
        "unit": "kg"
      },
      "dimensions": {
        "length": 20,
        "width": 15,
        "height": 10,
        "unit": "cm"
      }
    }
  }
}
```

### Publishing Events

**ShipmentDispatched Event**:
```json
{
  "specversion": "1.0",
  "type": "com.example.fulfillment.shipment.dispatched",
  "source": "/fulfillment/shipment-transport-service",
  "subject": "550e8400-e29b-41d4-a716-446655440001",
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "time": "2023-12-01T11:00:00Z",
  "datacontenttype": "application/json",
  "data": {
    "shipment_id": "550e8400-e29b-41d4-a716-446655440001",
    "order_id": "987fcdeb-51a2-43d1-9c4f-123456789abc",
    "carrier_name": "FedEx",
    "tracking_number": "784398712345",
    "dispatched_at": "2023-12-01T11:00:00Z"
  }
}
```

**ShipmentDelivered Event**:
```json
{
  "specversion": "1.0",
  "type": "com.example.fulfillment.shipment.delivered",
  "source": "/fulfillment/shipment-transport-service",
  "subject": "550e8400-e29b-41d4-a716-446655440001",
  "id": "770e8400-e29b-41d4-a716-446655440001",
  "time": "2023-12-03T14:30:00Z",
  "datacontenttype": "application/json",
  "data": {
    "shipment_id": "550e8400-e29b-41d4-a716-446655440001",
    "order_id": "987fcdeb-51a2-43d1-9c4f-123456789abc",
    "delivered_at": "2023-12-03T14:30:00Z"
  }
}
```

## 🧪 Testing

### Running Tests

```bash
# Unit tests only
./mvnw test

# Integration tests
./mvnw verify -Dtest.profile=integration

# All tests with coverage
./mvnw clean verify

# View coverage report
open target/site/jacoco/index.html
```

### Test Categories

- **Unit Tests**: Domain logic, value objects, services
- **Integration Tests**: Repository, messaging, external APIs
- **Contract Tests**: Carrier API integration with WireMock

### Testing with TestContainers

Integration tests use TestContainers for:
- MongoDB (real database operations)
- Kafka (event publishing/consuming)
- WireMock (external API simulation)

## 📊 Monitoring & Observability

### Metrics Endpoints

- **Health**: `/api/management/health`
- **Metrics**: `/api/management/metrics`
- **Prometheus**: `/api/management/prometheus`
- **Info**: `/api/management/info`

### Custom Health Indicators

- **Carriers**: Checks connectivity to shipping carrier APIs
- **Outbox**: Monitors unpublished events in outbox pattern
- **MongoDB**: Database connectivity and performance
- **Kafka**: Message broker connectivity

### Key Metrics to Monitor

```bash
# Application metrics
curl http://localhost:8080/api/management/metrics/shipment.created.total
curl http://localhost:8080/api/management/metrics/carrier.selection.duration
curl http://localhost:8080/api/management/metrics/outbox.events.pending

# JVM metrics
curl http://localhost:8080/api/management/metrics/jvm.memory.used
curl http://localhost:8080/api/management/metrics/jvm.gc.pause
```

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MONGODB_URI` | MongoDB connection string | `mongodb://localhost:27017/shipment_transport` |
| `KAFKA_BROKERS` | Kafka broker addresses | `localhost:9092` |
| `FEDEX_API_URL` | FedEx API endpoint | `https://apis-sandbox.fedex.com` |
| `FEDEX_API_KEY` | FedEx API key | Required |
| `FEDEX_ACCOUNT_NUMBER` | FedEx account number | Required |
| `TRACKING_JOB_ENABLED` | Enable background tracking job | `true` |
| `TRACKING_JOB_INTERVAL` | Tracking job interval (ms) | `3600000` (1 hour) |

### Profiles

- **local**: Development with external services on localhost
- **docker**: Running in Docker Compose environment
- **production**: Production configuration with external dependencies

### Carrier Configuration

```yaml
carriers:
  fedex:
    api-url: ${FEDEX_API_URL}
    api-key: ${FEDEX_API_KEY}
    account-number: ${FEDEX_ACCOUNT_NUMBER}
  ups:
    api-url: ${UPS_API_URL}
    api-key: ${UPS_API_KEY}
    account-number: ${UPS_ACCOUNT_NUMBER}
```

## 📁 Project Structure

```
src/main/java/com/example/shipment/
├── domain/                     # Core business logic
│   ├── model/                 # Aggregates, Entities, Value Objects
│   │   ├── aggregate/         # Shipment aggregate
│   │   ├── entity/            # TrackingEvent entity
│   │   ├── valueobject/       # All value objects
│   │   └── exception/         # Domain exceptions
│   ├── service/               # Domain services
│   ├── repository/            # Repository interfaces (ports)
│   └── port/                  # Adapter interfaces
├── application/               # Use cases and application services
│   ├── service/               # Application services
│   ├── port/                  # Application ports
│   └── dto/                   # Application DTOs
├── infrastructure/            # External concerns and adapters
│   ├── persistence/           # MongoDB implementations
│   ├── carrier/               # Carrier API adapters
│   ├── messaging/             # CloudEvents and Kafka
│   ├── outbox/                # Transactional outbox pattern
│   ├── job/                   # Scheduled jobs
│   └── configuration/         # Infrastructure configuration
└── adapter/                   # External interfaces
    ├── web/                   # REST controllers
    └── messaging/             # Event consumers
```

## 🛠️ Development Workflows

### Adding a New Carrier

1. **Create Adapter Implementation**
```java
@Component
public class UpsAdapter implements ICarrierAdapter {
    // Implement UPS-specific API integration
}
```

2. **Add Configuration**
```yaml
carriers:
  ups:
    api-url: ${UPS_API_URL}
    api-key: ${UPS_API_KEY}
```

3. **Create Tests**
```java
@ExtendWith(MockitoExtension.class)
class UpsAdapterTest {
    // Test UPS-specific functionality
}
```

### Extending Tracking Information

1. **Update TrackingEvent Entity** (if needed)
2. **Modify TrackingEventDocument** for persistence
3. **Update API Response DTOs**
4. **Add tests for new functionality**

### Custom Carrier Selection Strategy

```java
@Component
public class FastestDeliveryStrategy implements CarrierSelectionStrategy {
    
    @Override
    public CarrierSelectionResult selectCarrier(Package packageInfo, List<ICarrierAdapter> adapters) {
        // Implement fastest delivery logic
        return new CarrierSelectionResult(selectedCarrier, cost, reason);
    }
}
```

## 🚢 Deployment

### Container Deployment

```bash
# Build image
docker build -t shipment-transport:latest .

# Run container
docker run -d \
  -p 8080:8080 \
  -e MONGODB_URI=mongodb://mongodb:27017/shipment_transport \
  -e KAFKA_BROKERS=kafka:9092 \
  shipment-transport:latest
```

### Kubernetes Deployment

```bash
# Apply manifests
kubectl apply -f k8s/

# Check deployment status
kubectl get pods -n shipment-transport

# View logs
kubectl logs -f deployment/shipment-transport -n shipment-transport
```

### Production Considerations

- **Resource Limits**: Set appropriate CPU/memory limits
- **Health Checks**: Configure liveness and readiness probes
- **Secrets Management**: Use Kubernetes secrets for API keys
- **Monitoring**: Set up Prometheus metrics scraping
- **Logging**: Configure structured logging with correlation IDs

## 🤝 Contributing

### Development Setup

1. Fork the repository
2. Create a feature branch
3. Make your changes following the architecture patterns
4. Ensure tests pass: `./mvnw verify`
5. Submit a pull request

### Code Standards

- Follow Domain-Driven Design principles
- Maintain hexagonal architecture boundaries
- Write comprehensive tests (unit + integration)
- Use meaningful commit messages
- Update documentation for new features

### Architecture Decision Records

Major architectural decisions are documented in `/docs/adr/`:
- ADR-001: Hexagonal Architecture Choice
- ADR-002: Transactional Outbox Pattern
- ADR-003: CloudEvents Standard Adoption

## 📚 Additional Resources

- [Architecture Guide](ARCHITECTURE.md) - Detailed architectural overview
- [Domain Implementation Guide](DOMAIN_IMPLEMENTATION_GUIDE.md) - Domain layer patterns
- [Testing Guide](TESTING_GUIDE.md) - Testing strategies and examples
- [Configuration Guide](CONFIGURATION_DEPLOYMENT_GUIDE.md) - Setup and deployment
- [OpenAPI Specification](openapi.yaml) - REST API contract
- [AsyncAPI Specification](asyncapi.yaml) - Event contracts

## 📜 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🆘 Support

For questions, issues, or contributions:
- Create an issue in the repository
- Contact the development team
- Check the documentation in the `/docs` directory

---

**Built with ❤️ using Spring Boot, Domain-Driven Design, and Hexagonal Architecture**