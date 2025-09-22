# Architectural Overview

This mini-guide maps out the architectural shape of the Shipment & Transportation service. It highlights the intent behind the Domain-Driven Design (DDD) building blocks and shows how Hexagonal Architecture keeps the core isolated from the outside world.

## How to Use These Docs

- Start with the quick summaries below to decide where to dive deeper.
- Use the diagrams to visualise relationships; each file links back here for context.
- When introducing new capabilities, revisit the responsibilities and invariants so that changes stay aligned with the existing model.

## Architecture Topics

| Topic | Focus |
| --- | --- |
| [Domain Aggregates](./aggregates.md) | Consistency boundaries for `Shipment` and `Load`, their invariants, and collaboration rules. |
| [Application Services](./application-services.md) | How use cases orchestrate aggregates through ports and enforce Hexagonal isolation. |
| [Value Objects](./value-objects.md) | Immutable domain concepts that add meaning, validation, and expressiveness. |
| [Infrastructure Layer](./infrastructure-layer.md) | Ports versus adapters, and how technology-specific code stays outside the domain. |

## Core Principles

- **Domain-Driven Design:** Capture business language in aggregates, entities, and value objects that protect invariants and express intent.
- **Hexagonal Architecture:** Define clean ports, isolate adapters, and keep the application core agnostic of frameworks and infrastructure.
- **Separation of Concerns:** Let higher layers coordinate work while deeper layers own business rules and technology integration.

Continue with any topic using the links above. Each page closes with pointers back to related sections so you can move between perspectives without losing context. Jump back here from elsewhere via the [Docs Index](../README.md).
