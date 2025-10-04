# Documentation Index

Use this index to navigate the Shipment & Transportation documentation. It groups the core guides, architecture deep dives, and reference material so you can quickly find the right context for any change.

## Core Guides

- [ARCHITECTURE.md](ARCHITECTURE.md) – End-to-end view of the service boundaries and key flows.
- [DOMAIN_IMPLEMENTATION_GUIDE.md](DOMAIN_IMPLEMENTATION_GUIDE.md) – Practical rules for modelling aggregates, entities, and value objects.
- [APPLICATION_INFRASTRUCTURE_GUIDE.md](APPLICATION_INFRASTRUCTURE_GUIDE.md) – Details on supporting services, messaging, and observability.
- [CONFIGURATION_DEPLOYMENT_GUIDE.md](CONFIGURATION_DEPLOYMENT_GUIDE.md) – Environment variables, secrets, and deployment pipeline notes.
- [DOCKER_GUIDE.md](DOCKER_GUIDE.md) – Local container workflows and troubleshooting tips.
- [TESTING_GUIDE.md](TESTING_GUIDE.md) – Strategy, tooling, and conventions for automated tests.

## Architecture Deep Dives

- [Overview](./architecture/README.md) – How the architecture documentation is structured plus quick links to each topic.
- [Domain Aggregates](./architecture/aggregates.md) – Consistency boundaries, invariants, and collaboration rules for `Shipment` and `Load`.
- [Application Services](./architecture/application-services.md) – Use-case orchestration and port-driven design.
- [Value Objects](./architecture/value-objects.md) – Patterns for expressive, validated domain types.
- [Infrastructure Layer](./architecture/infrastructure-layer.md) – Driving versus driven adapters and how technology stays outside the core.

## API Specifications

- [OpenAPI](../openapi.yaml) – REST surface for synchronous integrations.
- [AsyncAPI](../asyncapi.yaml) – Event contracts for asynchronous messaging.

Need another topic? Add it here so future readers can discover it from a single place.
