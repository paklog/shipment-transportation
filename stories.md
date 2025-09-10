# Epic 2: Carrier Integration & Label Generation
**Goal:** To build the crucial capability of communicating with external shipping carriers to select the best service, generate shipping labels, and obtain tracking numbers.

| Story ID | User Story | Acceptance Criteria |
|----------|------------|----------------------|
| **ST-03** | As a Developer, I want to define a generic `ICarrierAdapter` interface so that the system can interact with any carrier (FedEx, UPS, etc.) in a consistent way. | - An interface (`ICarrierAdapter`) is defined with methods like `createShipment(package)` and `getTrackingStatus(trackingNumber)`.<br>- This promotes a pluggable architecture for adding new carriers in the future. |
| **ST-04** | As a Developer, I want to implement a concrete adapter for our primary shipping carrier so that the system can generate real shipping labels and tracking numbers. | - A `FedExAdapter` (or similar) class is created that implements `ICarrierAdapter`.<br>- The adapter correctly formats requests and parses responses from the carrier's external API. |
| **ST-05** | As the System, I want to implement a "Carrier Selection" domain service that automatically chooses the most cost-effective carrier for a given package and shipping speed. | - A `CarrierSelectionService` is created.<br>- It contains business logic to compare rates and delivery times from all available carrier adapters.<br>- It returns the best carrier for a given package, based on business rules. |
| **ST-06** | As the System, when a package is ready, I want to use the selected carrier to create a shipment and retrieve a label so that the package can be physically dispatched from the warehouse. | - The system calls the `CarrierSelectionService` to get the best carrier.<br>- It then calls the `createShipment()` method on the chosen carrier's adapter.<br>- The returned `tracking_number` and `label_data` are used to create and persist a new Shipment aggregate. |

---

# Epic 3: Shipment Tracking & Status Visibility
**Goal:** To provide both internal users and the system itself with up-to-date information on a shipment's location and status throughout its journey.

| Story ID | User Story | Acceptance Criteria |
|----------|------------|----------------------|
| **ST-07** | As a Developer, I want to create a scheduled background job that periodically checks for tracking updates for all "In Transit" shipments. | - A recurring job (e.g., a cron job) is configured to run at a set interval (e.g., every hour). |
| **ST-08** | As the System, during the tracking job, I want to query the appropriate carrier's API for the latest status of each active shipment so that our internal records are kept up-to-date. | - The job fetches all Shipment aggregates with a status of *InTransit*.<br>- For each shipment, it calls the `getTrackingStatus()` method on the corresponding `ICarrierAdapter`.<br>- Any new tracking events (e.g., "Arrived at hub," "Out for delivery") are added to the Shipment aggregate and persisted. |
| **ST-09** | As a Logistics Coordinator, I want an API endpoint to retrieve the full tracking history for a specific shipment so that I can answer customer inquiries about delivery status. | - A `GET /shipments/{shipment_id}/tracking` endpoint is created.<br>- It returns the complete Shipment aggregate, including its list of all recorded tracking events. |

---

# Epic 4: System Integration & Event Communication
**Goal:** To ensure the service is a good citizen in our event-driven ecosystem, correctly consuming upstream events and reliably publishing its own business-critical domain events.

| Story ID | User Story | Acceptance Criteria |
|----------|------------|----------------------|
| **ST-10** | As the System, I want to consume `PackagePacked` events from Kafka so that I know when a new package is ready for shipment processing. | - A Kafka consumer is implemented in the service.<br>- The consumer is subscribed to the `fulfillment.warehouse.v1.events` topic.<br>- On receiving a `PackagePacked` event, it triggers the carrier selection and label generation process (Epic 2). |
| **ST-11** | As the System, when a shipping label and tracking number have been successfully generated, I want to publish a `ShipmentDispatched` event so that Order Management and other contexts know the order is officially on its way to the customer. | - After a Shipment aggregate is successfully created, a `ShipmentDispatched` event is generated.<br>- The event is structured as a valid **CloudEvent** and contains the `order_id`, `shipment_id`, `carrier_name`, and `tracking_number`.<br>- The event is published to a new Kafka topic (e.g., `fulfillment.shipment.v1.events`). |
| **ST-12** | As the System, when the tracking status for a shipment is updated to "Delivered," I want to publish a `ShipmentDelivered` event to signal the successful completion of the entire fulfillment journey. | - When the tracking job (ST-08) detects a final "Delivered" status, a `ShipmentDelivered` event is generated.<br>- The event is structured as a valid **CloudEvent** and contains the `order_id` and `shipment_id`.<br>- The event is published to the `fulfillment.shipment.v1.events` topic. |
| **ST-13** | As a Developer, I want to implement the Transactional Outbox pattern for publishing events so that we guarantee that events are sent if and only if the business transaction was successfully committed to our database. | - An outbox table is added to the service's database.<br>- Saving an aggregate (e.g., Shipment) and writing its corresponding event to the outbox table occur in the same database transaction.<br>- A separate worker process reliably reads from the outbox and publishes events to Kafka, ensuring no events are lost. |
