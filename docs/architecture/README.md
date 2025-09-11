# Architectural Overview

This documentation provides a deep dive into the architecture of the Shipment & Transportation service, guided by the principles of Domain-Driven Design (DDD) and Hexagonal Architecture (Ports and Adapters).

Our system is modeled around a rich domain core, which is decoupled from external concerns such as the database, messaging queues, and third-party APIs. This separation of concerns makes the system more resilient, testable, and easier to maintain over time.

## Core Principles

- **Domain-Driven Design:** We focus on modeling a rich, expressive domain that accurately reflects the business processes. Key building blocks include Aggregates, Entities, and Value Objects.
- **Hexagonal Architecture:** The application core is isolated from the outside world. All external interactions happen through well-defined "Ports" (interfaces), which are implemented by "Adapters" in the infrastructure layer.

## Further Reading

- [Domain Aggregates](./aggregates.md)
- [Application Services & Hexagonal Architecture](./application-services.md)
- [Key Value Objects](./value-objects.md)
- [The Infrastructure Layer](./infrastructure-layer.md)
