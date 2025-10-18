---
layout: default
title: Home
---

# Shipment & Transportation Service Documentation

Carrier integration and shipment tracking service with DDD, hexagonal architecture, CloudEvents, and transactional outbox pattern.

## Overview

The Shipment & Transportation Service manages shipment creation, carrier integration, and tracking within the Paklog fulfillment platform. This bounded context handles carrier selection, load tendering, shipping label generation, and real-time tracking updates.

## Quick Links

### Getting Started
- [Architecture Overview](ARCHITECTURE.md) - System architecture
- [Domain Implementation Guide](DOMAIN_IMPLEMENTATION_GUIDE.md) - Domain layer details
- [Configuration & Deployment](CONFIGURATION_DEPLOYMENT_GUIDE.md) - Setup guide

### Development
- [Testing Guide](TESTING_GUIDE.md) - Testing strategies
- [Docker Guide](DOCKER_GUIDE.md) - Containerization
- [Persistence & Events Guide](PERSISTENCE_EVENTS_GUIDE.md) - Data and events
- [Application Infrastructure](APPLICATION_INFRASTRUCTURE_GUIDE.md) - Infrastructure layer

### Architecture Details
- [Aggregates](architecture/aggregates.md) - Domain aggregates
- [Value Objects](architecture/value-objects.md) - Domain value objects
- [Application Services](architecture/application-services.md) - Service layer
- [Infrastructure Layer](architecture/infrastructure-layer.md) - External integrations

### Security
- [Authentication](security/AUTHENTICATION.md) - Auth mechanisms
- [Authorization](security/AUTHORIZATION.md) - Access control
- [External API Security](security/EXTERNAL_API_SECURITY.md) - Carrier API security

### Monitoring
- [Grafana Dashboard](GRAFANA_DASHBOARD.md) - Metrics and monitoring

## Technology Stack

- **Java 21** - Programming language
- **Spring Boot 3.2+** - Application framework
- **MongoDB** - Document database
- **Apache Kafka** - Event streaming
- **CloudEvents** - Event standard
- **OpenTelemetry** - Distributed tracing
- **WireMock** - API testing

## Key Features

- Carrier integration (FedEx, UPS)
- Shipment tracking
- Load tendering
- Label generation
- Real-time tracking updates
- Multiple carrier support
- Hexagonal architecture
- Event-driven design

## Domain Model

### Aggregates
- **Shipment** - Shipment lifecycle management
- **Load** - Carrier tender/booking

### Entities
- **TrackingEvent** - Tracking updates
- **Tender** - Carrier tender request
- **Pickup** - Pickup scheduling

### Value Objects
- **ShipmentId**, **LoadId** - Identifiers
- **TrackingNumber** - Carrier tracking
- **CarrierName** - Carrier identifier
- **Package** - Package details
- **Location** - Geographic location
- **ShippingCost** - Cost information

### Domain Events
- **PackagePackedEvent** (consumed)
- **LoadCreatedEvent**
- **ShipmentDispatchedEvent**
- **ShipmentDeliveredEvent**

### Domain Services
- **CarrierSelectionService** - Optimal carrier selection
- **DefaultCarrierSelectionStrategy** - Selection algorithm

## Architecture Patterns

- **Hexagonal Architecture** - Ports and adapters
- **Domain-Driven Design** - Rich domain model
- **Event-Driven Architecture** - Async integration
- **Transactional Outbox** - Reliable events
- **Adapter Pattern** - Unified carrier interface
- **Strategy Pattern** - Pluggable selection

## Carrier Integration

### Supported Carriers
- **FedEx** - Full integration
- **UPS** - Full integration

### Adding New Carriers
1. Implement `ICarrierAdapter`
2. Add configuration
3. Register in Spring context
4. Add integration tests

## API Endpoints

- `GET /api/v1/shipments/{shipmentId}/tracking` - Get tracking
- `GET /api/management/health` - Health check
- `GET /api/management/metrics` - Metrics
- `GET /api/management/prometheus` - Prometheus metrics

## Getting Started

1. Review the [Architecture Overview](ARCHITECTURE.md)
2. Read the [Domain Implementation Guide](DOMAIN_IMPLEMENTATION_GUIDE.md)
3. Follow the [Configuration & Deployment Guide](CONFIGURATION_DEPLOYMENT_GUIDE.md)
4. Study the [Testing Guide](TESTING_GUIDE.md)
5. Set up [Security](security/) components

## Integration Points

### Consumes Events From
- Warehouse Operations (package packed)

### Publishes Events To
- Order Management (shipment dispatched/delivered)
- Inventory (shipment updates)

## Monitoring

- Health checks with carrier status
- Prometheus metrics
- OpenTelemetry tracing
- Grafana dashboards
- Custom business metrics

## Contributing

For contribution guidelines, please refer to the main [README](../README.md) in the project root.

## Support

- **GitHub Issues**: [Report bugs or request features](https://github.com/paklog/shipment-transportation/issues)
- **Documentation**: Browse the guides above
- **Runbooks**: See architecture documentation
