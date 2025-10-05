# Shipment & Transportation API Guide

This document provides a comprehensive guide for developers using the Shipment & Transportation API. It details the available endpoints, their functionalities, and how to interact with them.

## Base URL

All API endpoints are relative to the following base URL:

```
http://localhost:8080
```

## Authentication

The API does not currently require authentication.

---

## Shipments API

The Shipments API provides endpoints for managing and tracking individual shipments throughout their lifecycle.

### List Shipments

- **`GET /shipments`**

Retrieves a paginated collection of shipments, which can be filtered by status and carrier. This is useful for monitoring freight and delivery progress.

**Query Parameters:**

- `status` (optional): Filters shipments by status (e.g., `CREATED`, `IN_TRANSIT`, `DELIVERED`).
- `carrierName` (optional): Filters shipments by the assigned carrier (e.g., `FEDEX`, `UPS`).
- `page` (optional): The page number to retrieve (default: `0`).
- `size` (optional): The number of shipments per page (default: `20`).

**Responses:**

- `200 OK`: A paginated list of shipments.
- `400 Bad Request`: The request was malformed.
- `500 Internal Server Error`: An unexpected error occurred.

**Example Response (`200 OK`):**

```json
{
  "items": [
    {
      "id": {
        "value": "1f243a5d-e2b4-4a27-8ec6-9bb3c3e2f4c1"
      },
      "orderId": {
        "value": "ORD-100045"
      },
      "status": "UNASSIGNED",
      "carrierName": null,
      "trackingNumber": {
        "value": "TRK123456789"
      },
      "trackingEvents": [
        {
          "status": "CREATED",
          "statusDescription": "Shipment registered in transportation platform",
          "location": "Portland, OR",
          "timestamp": "2024-05-01T09:30:00Z",
          "eventCode": "CREATED",
          "detailedDescription": "Shipment created and awaiting load assignment"
        }
      ],
      "assignedLoadId": null,
      "lastUpdatedAt": "2024-05-01T09:30:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

### Get Shipment by ID

- **`GET /shipments/{shipmentId}`**

Retrieves the details of a specific shipment, including its order information, assigned load, and tracking history.

**Path Parameters:**

- `shipmentId` (required): The unique identifier of the shipment.

**Headers:**

- `If-None-Match` (optional): An ETag value for caching. If the shipment has not changed, a `304 Not Modified` response is returned.

**Responses:**

- `200 OK`: The shipment details.
- `304 Not Modified`: The shipment has not been modified since the last request.
- `404 Not Found`: The shipment with the specified ID was not found.
- `500 Internal Server Error`: An unexpected error occurred.

**Example Response (`200 OK`):**

```json
{
  "id": {
    "value": "1f243a5d-e2b4-4a27-8ec6-9bb3c3e2f4c1"
  },
  "orderId": {
    "value": "ORD-100045"
  },
  "status": "IN_TRANSIT",
  "carrierName": "FEDEX",
  "trackingNumber": {
    "value": "TRK123456789"
  },
  "trackingEvents": [
    {
      "status": "CREATED",
      "statusDescription": "Shipment registered in transportation platform",
      "location": "Portland, OR",
      "timestamp": "2024-05-01T09:30:00Z",
      "eventCode": "CREATED",
      "detailedDescription": "Shipment created and awaiting load assignment"
    },
    {
      "status": "IN_TRANSIT",
      "statusDescription": "Package departed origin facility",
      "location": "Portland, OR",
      "timestamp": "2024-05-02T15:10:12Z",
      "eventCode": "DEPARTED_ORIGIN",
      "detailedDescription": "Carrier has picked up the freight and departed origin terminal"
    }
  ],
  "assignedLoadId": "8c9a5c1e-2f5b-4ad3-9ade-5e6fbe9e2e3f",
  "lastUpdatedAt": "2024-05-02T15:10:12Z"
}
```

---

## Loads API

The Loads API is used to create, manage, and track transportation loads, which are aggregations of one or more shipments.

### List Loads

- **`GET /loads`**

Provides a paginated view of transportation loads, which can be filtered by status and carrier.

**Query Parameters:**

- `status` (optional): Filters loads by status (e.g., `PLANNED`, `TENDERED`, `IN_TRANSIT`).
- `carrierName` (optional): Filters loads by the assigned carrier.
- `page` (optional): The page number to retrieve (default: `0`).
- `size` (optional): The number of loads per page (default: `20`).

**Responses:**

- `200 OK`: A paginated list of loads.
- `400 Bad Request`: The request was malformed.
- `500 Internal Server Error`: An unexpected error occurred.

### Create a New Load

- **`POST /loads`**

Creates a new transportation load to orchestrate shipments between an origin and a destination.

**Request Body:**

- `reference` (string, required): A user-defined reference for the load.
- `shipments` (array of strings, required): A list of shipment IDs to be included in the load.
- `origin` (Location object, required): The starting point of the load.
- `destination` (Location object, required): The final destination of the load.
- `requestedPickupDate` (string, required): The planned pickup date in `YYYY-MM-DD` format.
- `requestedDeliveryDate` (string, required): The planned delivery date in `YYYY-MM-DD` format.
- `notes` (string, optional): Additional notes for the load.

**Responses:**

- `201 Created`: The load was created successfully.
- `400 Bad Request`: The request was malformed.
- `409 Conflict`: The load could not be created due to a conflict.
- `500 Internal Server Error`: An unexpected error occurred.

### Get Load Details

- **`GET /loads/{loadId}`**

Retrieves the complete details of a load, including shipment assignments, carrier information, and tender history.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Headers:**

- `If-None-Match` (optional): An ETag value for caching.

**Responses:**

- `200 OK`: The load details.
- `304 Not Modified`: The load has not been modified.
- `404 Not Found`: The load was not found.
- `500 Internal Server Error`: An unexpected error occurred.

### Update a Load

- **`PATCH /loads/{loadId}`**

Applies partial updates to an existing load, such as changing dates, notes, or status.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Headers:**

- `If-Match` (optional): An ETag value for optimistic concurrency control.

**Request Body:**

- `status` (string, optional): The new status of the load.
- `requestedPickupDate` (string, optional): The updated pickup date.
- `requestedDeliveryDate` (string, optional): The updated delivery date.
- `notes` (string, optional): Updated notes.

**Responses:**

- `200 OK`: The load was updated successfully.
- `400 Bad Request`: The request was malformed.
- `404 Not Found`: The load was not found.
- `409 Conflict`: A conflict occurred during the update.
- `412 Precondition Failed`: The `If-Match` header did not match.
- `500 Internal Server Error`: An unexpected error occurred.

### Cancel a Load

- **`DELETE /loads/{loadId}`**

Cancels a planned or in-progress load.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Headers:**

- `If-Match` (optional): An ETag value for optimistic concurrency control.

**Responses:**

- `204 No Content`: The load was successfully canceled.
- `404 Not Found`: The load was not found.
- `409 Conflict`: The load could not be canceled due to its current state.
- `412 Precondition Failed`: The `If-Match` header did not match.
- `500 Internal Server Error`: An unexpected error occurred.

---

## Load Shipments API

This API manages the assignment of shipments to loads.

### List Shipments for a Load

- **`GET /loads/{loadId}/shipments`**

Retrieves the shipments currently assigned to a specific load.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Query Parameters:**

- `page` (optional): The page number to retrieve.
- `size` (optional): The number of shipments per page.

**Responses:**

- `200 OK`: A paginated list of shipments for the load.
- `404 Not Found`: The load was not found.
- `500 Internal Server Error`: An unexpected error occurred.

### Assign Shipments to a Load

- **`POST /loads/{loadId}/shipments`**

Adds one or more shipments to a specified load.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Request Body:**

- `shipmentIds` (array of strings, required): The IDs of the shipments to assign.

**Responses:**

- `201 Created`: The shipments were successfully assigned.
- `400 Bad Request`: The request was malformed.
- `404 Not Found`: The load was not found.
- `409 Conflict`: A conflict occurred during assignment.
- `500 Internal Server Error`: An unexpected error occurred.

### Remove a Shipment from a Load

- **`DELETE /loads/{loadId}/shipments/{shipmentId}`**

Removes a shipment from a load.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.
- `shipmentId` (required): The unique identifier of the shipment to remove.

**Responses:**

- `204 No Content`: The shipment was successfully removed.
- `404 Not Found`: The load or shipment was not found.
- `409 Conflict`: The shipment could not be removed due to the load's state.
- `500 Internal Server Error`: An unexpected error occurred.

---

## Load Carrier API

This API is used to assign and manage carriers for loads.

### Assign a Carrier to a Load

- **`PUT /loads/{loadId}/carrier`**

Assigns a carrier to a load and prepares it for tendering.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Headers:**

- `If-Match` (optional): An ETag value for optimistic concurrency control.

**Request Body:**

- `carrierName` (string, required): The name of the carrier to assign.
- `scac` (string, optional): The Standard Carrier Alpha Code.
- `contactName` (string, optional): The contact person at the carrier.
- `contactPhone` (string, optional): The contact phone number.

**Responses:**

- `200 OK`: The carrier was assigned successfully.
- `400 Bad Request`: The request was malformed.
- `404 Not Found`: The load was not found.
- `409 Conflict`: A conflict occurred during assignment.
- `412 Precondition Failed`: The `If-Match` header did not match.
- `500 Internal Server Error`: An unexpected error occurred.

### Unassign a Carrier from a Load

- **`DELETE /loads/{loadId}/carrier`**

Removes the carrier assignment from a load.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Headers:**

- `If-Match` (optional): An ETag value for optimistic concurrency control.

**Responses:**

- `204 No Content`: The carrier was unassigned successfully.
- `404 Not Found`: The load was not found.
- `409 Conflict`: The carrier could not be unassigned.
- `412 Precondition Failed`: The `If-Match` header did not match.
- `500 Internal Server Error`: An unexpected error occurred.

---

## Load Rating API

This API provides cost calculations for loads.

### Get the Calculated Rate for a Load

- **`GET /loads/{loadId}/rate`**

Calculates the transportation cost for a load based on the assigned carrier and other factors.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Responses:**

- `200 OK`: The calculated shipping cost.
- `404 Not Found`: The load was not found.
- `409 Conflict`: The rate could not be calculated.
- `500 Internal Server Error`: An unexpected error occurred.

**Example Response (`200 OK`):**

```json
{
  "amount": 1250.75,
  "currency": "USD",
  "estimatedDeliveryDays": 3
}
```

---

## Tender API

The Tender API manages the process of offering a load to a carrier.

### Get Tender Details

- **`GET /loads/{loadId}/tender`**

Retrieves the tender details for a load, including its status and expiration.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Responses:**

- `200 OK`: The tender details.
- `404 Not Found`: The load was not found.
- `500 Internal Server Error`: An unexpected error occurred.

### Tender a Load

- **`PUT /loads/{loadId}/tender`**

Creates or refreshes a tender offer to the assigned carrier.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Headers:**

- `If-Match` (optional): An ETag value for optimistic concurrency control.

**Request Body:**

- `expiresAt` (string, required): The expiration date and time of the tender in `YYYY-MM-DDTHH:mm:ssZ` format.
- `notes` (string, optional): Additional notes for the tender.

**Responses:**

- `200 OK`: The tender was refreshed successfully.
- `201 Created`: The tender was created successfully.
- `400 Bad Request`: The request was malformed.
- `404 Not Found`: The load was not found.
- `409 Conflict`: A conflict occurred.
- `412 Precondition Failed`: The `If-Match` header did not match.
- `500 Internal Server Error`: An unexpected error occurred.

### Withdraw a Tender

- **`DELETE /loads/{loadId}/tender`**

Cancels an active tender offer.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Headers:**

- `If-Match` (optional): An ETag value for optimistic concurrency control.

**Responses:**

- `204 No Content`: The tender was successfully withdrawn.
- `404 Not Found`: The load was not found.
- `409 Conflict`: The tender could not be withdrawn.
- `412 Precondition Failed`: The `If-Match` header did not match.
- `500 Internal Server Error`: An unexpected error occurred.

### Record Tender Decision

- **`PUT /loads/{loadId}/tender/decision`**

Records the carrier's response (accept or decline) to a tender.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Headers:**

- `If-Match` (optional): An ETag value for optimistic concurrency control.

**Request Body:**

- `decision` (string, required): The carrier's decision (`ACCEPTED` or `DECLINED`).
- `respondedBy` (string, optional): The person who responded to the tender.
- `reason` (string, optional): The reason for the decision.

**Responses:**

- `200 OK`: The decision was recorded successfully.
- `400 Bad Request`: The request was malformed.
- `404 Not Found`: The load was not found.
- `409 Conflict`: A conflict occurred.
- `412 Precondition Failed`: The `If-Match` header did not match.
- `500 Internal Server Error`: An unexpected error occurred.

---

## Pickup API

The Pickup API is used to schedule and manage pickup appointments for loads.

### Get Pickup Details

- **`GET /loads/{loadId}/pickup`**

Provides the confirmed pickup appointment and contact information for a load.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Responses:**

- `200 OK`: The pickup details.
- `404 Not Found`: The load was not found.
- `500 Internal Server Error`: An unexpected error occurred.

### Schedule or Update a Pickup

- **`PUT /loads/{loadId}/pickup`**

Creates or updates the pickup appointment for a load.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Headers:**

- `If-Match` (optional): An ETag value for optimistic concurrency control.

**Request Body:**

- `scheduledFor` (string, required): The scheduled pickup time in `YYYY-MM-DDTHH:mm:ssZ` format.
- `location` (Location object, required): The pickup location.
- `contactName` (string, optional): The name of the contact person at the pickup location.
- `contactPhone` (string, optional): The phone number of the contact person.
- `instructions` (string, optional): Special instructions for the pickup.

**Responses:**

- `200 OK`: The pickup was updated successfully.
- `201 Created`: The pickup was scheduled successfully.
- `400 Bad Request`: The request was malformed.
- `404 Not Found`: The load was not found.
- `409 Conflict`: A conflict occurred.
- `412 Precondition Failed`: The `If-Match` header did not match.
- `500 Internal Server Error`: An unexpected error occurred.

### Cancel a Pickup

- **`DELETE /loads/{loadId}/pickup`**

Cancels a scheduled pickup appointment.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Headers:**

- `If-Match` (optional): An ETag value for optimistic concurrency control.

**Responses:**

- `204 No Content`: The pickup was canceled successfully.
- `404 Not Found`: The load was not found.
- `409 Conflict`: The pickup could not be canceled.
- `412 Precondition Failed`: The `If-Match` header did not match.
- `500 Internal Server Error`: An unexpected error occurred.

---

## Documents API

The Documents API provides access to operational documents related to loads.

### Get Bill of Lading

- **`GET /loads/{loadId}/bol`**

Retrieves the Bill of Lading (BOL) document for a load.

**Path Parameters:**

- `loadId` (required): The unique identifier of the load.

**Responses:**

- `200 OK`: The Bill of Lading content as plain text.
- `404 Not Found`: The load was not found.
- `500 Internal Server Error`: An unexpected error occurred.
