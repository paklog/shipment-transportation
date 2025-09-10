# Domain Layer Implementation Guide

This guide provides detailed implementation examples for all domain layer components (Tasks 5-24 in the todo list).

## Value Objects Implementation

### ShipmentId (Task 5)
```java
package com.example.shipment.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

public record ShipmentId(UUID value) {
    public ShipmentId {
        Objects.requireNonNull(value, "Shipment ID cannot be null");
    }
    
    public static ShipmentId generate() {
        return new ShipmentId(UUID.randomUUID());
    }
    
    public static ShipmentId from(String value) {
        try {
            return new ShipmentId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid shipment ID format: " + value);
        }
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
```

### OrderId (Task 6)
```java
package com.example.shipment.domain.model.valueobject;

import java.util.Objects;
import java.util.UUID;

public record OrderId(UUID value) {
    public OrderId {
        Objects.requireNonNull(value, "Order ID cannot be null");
    }
    
    public static OrderId from(String value) {
        try {
            return new OrderId(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order ID format: " + value);
        }
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
```

### TrackingNumber (Task 7)
```java
package com.example.shipment.domain.model.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

public record TrackingNumber(String value) {
    private static final Pattern FEDEX_PATTERN = Pattern.compile("^[0-9]{12}$|^[0-9]{14}$");
    private static final Pattern UPS_PATTERN = Pattern.compile("^1Z[A-Z0-9]{16}$");
    
    public TrackingNumber {
        Objects.requireNonNull(value, "Tracking number cannot be null");
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Tracking number cannot be empty");
        }
        // Basic validation - more specific validation can be done per carrier
        if (!isValidFormat(value.trim())) {
            throw new IllegalArgumentException("Invalid tracking number format: " + value);
        }
        value = value.trim().toUpperCase();
    }
    
    private static boolean isValidFormat(String value) {
        return FEDEX_PATTERN.matcher(value).matches() || 
               UPS_PATTERN.matcher(value).matches();
    }
    
    public boolean isFedExFormat() {
        return FEDEX_PATTERN.matcher(value).matches();
    }
    
    public boolean isUpsFormat() {
        return UPS_PATTERN.matcher(value).matches();
    }
}
```

### CarrierName (Task 8)
```java
package com.example.shipment.domain.model.valueobject;

public enum CarrierName {
    FEDEX("FedEx"),
    UPS("UPS"),
    DHL("DHL"),
    USPS("USPS");
    
    private final String displayName;
    
    CarrierName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static CarrierName fromDisplayName(String displayName) {
        for (CarrierName carrier : values()) {
            if (carrier.displayName.equalsIgnoreCase(displayName)) {
                return carrier;
            }
        }
        throw new IllegalArgumentException("Unknown carrier: " + displayName);
    }
}
```

### Weight (Task 9)
```java
package com.example.shipment.domain.model.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Weight(BigDecimal value, WeightUnit unit) {
    public Weight {
        Objects.requireNonNull(value, "Weight value cannot be null");
        Objects.requireNonNull(unit, "Weight unit cannot be null");
        
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
        
        if (value.compareTo(new BigDecimal("1000")) > 0) {
            throw new IllegalArgumentException("Weight cannot exceed 1000 " + unit);
        }
        
        // Normalize to 2 decimal places
        value = value.setScale(2, RoundingMode.HALF_UP);
    }
    
    public Weight convertTo(WeightUnit targetUnit) {
        if (unit == targetUnit) {
            return this;
        }
        
        BigDecimal convertedValue = switch (unit) {
            case KG -> switch (targetUnit) {
                case LB -> value.multiply(new BigDecimal("2.20462"));
                case OZ -> value.multiply(new BigDecimal("35.274"));
                default -> value;
            };
            case LB -> switch (targetUnit) {
                case KG -> value.divide(new BigDecimal("2.20462"), 3, RoundingMode.HALF_UP);
                case OZ -> value.multiply(new BigDecimal("16"));
                default -> value;
            };
            case OZ -> switch (targetUnit) {
                case KG -> value.divide(new BigDecimal("35.274"), 3, RoundingMode.HALF_UP);
                case LB -> value.divide(new BigDecimal("16"), 3, RoundingMode.HALF_UP);
                default -> value;
            };
        };
        
        return new Weight(convertedValue, targetUnit);
    }
    
    public enum WeightUnit {
        KG("kg"), LB("lb"), OZ("oz");
        
        private final String symbol;
        
        WeightUnit(String symbol) {
            this.symbol = symbol;
        }
        
        public String getSymbol() {
            return symbol;
        }
    }
}
```

### Dimensions (Task 10)
```java
package com.example.shipment.domain.model.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

public record Dimensions(
    BigDecimal length, 
    BigDecimal width, 
    BigDecimal height, 
    DimensionUnit unit
) {
    public Dimensions {
        Objects.requireNonNull(length, "Length cannot be null");
        Objects.requireNonNull(width, "Width cannot be null");
        Objects.requireNonNull(height, "Height cannot be null");
        Objects.requireNonNull(unit, "Dimension unit cannot be null");
        
        if (length.compareTo(BigDecimal.ZERO) <= 0 ||
            width.compareTo(BigDecimal.ZERO) <= 0 ||
            height.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("All dimensions must be positive");
        }
    }
    
    public BigDecimal getVolume() {
        return length.multiply(width).multiply(height);
    }
    
    public BigDecimal getLongestSide() {
        return length.max(width.max(height));
    }
    
    public enum DimensionUnit {
        CM("cm"), IN("in"), MM("mm");
        
        private final String symbol;
        
        DimensionUnit(String symbol) {
            this.symbol = symbol;
        }
        
        public String getSymbol() {
            return symbol;
        }
    }
}
```

### Location (Task 11)
```java
package com.example.shipment.domain.model.valueobject;

import java.util.Objects;

public record Location(
    String city,
    String stateOrRegion,
    String postalCode,
    String countryCode
) {
    public Location {
        Objects.requireNonNull(city, "City cannot be null");
        Objects.requireNonNull(countryCode, "Country code cannot be null");
        
        if (city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be empty");
        }
        
        if (!isValidCountryCode(countryCode)) {
            throw new IllegalArgumentException("Invalid country code: " + countryCode);
        }
        
        city = city.trim();
        stateOrRegion = stateOrRegion != null ? stateOrRegion.trim() : null;
        postalCode = postalCode != null ? postalCode.trim() : null;
        countryCode = countryCode.trim().toUpperCase();
    }
    
    private static boolean isValidCountryCode(String countryCode) {
        return countryCode != null && 
               countryCode.trim().length() == 2 && 
               countryCode.trim().matches("[A-Z]{2}");
    }
}
```

### Package (Task 12)
```java
package com.example.shipment.domain.model.valueobject;

import java.util.Objects;

public record Package(
    PackageId packageId,
    OrderId orderId,
    Weight weight,
    Dimensions dimensions
) {
    public Package {
        Objects.requireNonNull(packageId, "Package ID cannot be null");
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        Objects.requireNonNull(weight, "Weight cannot be null");
        Objects.requireNonNull(dimensions, "Dimensions cannot be null");
    }
    
    public boolean isOversized() {
        // Business rule: package is oversized if any dimension > 100cm
        return dimensions.getLongestSide().compareTo(java.math.BigDecimal.valueOf(100)) > 0;
    }
    
    public boolean isHeavy() {
        // Business rule: package is heavy if weight > 25kg
        Weight twentyFiveKg = new Weight(java.math.BigDecimal.valueOf(25), Weight.WeightUnit.KG);
        return weight.convertTo(Weight.WeightUnit.KG)
                    .value()
                    .compareTo(twentyFiveKg.value()) > 0;
    }
}
```

## Enumerations

### ShipmentStatus (Task 13)
```java
package com.example.shipment.domain.model.valueobject;

public enum ShipmentStatus {
    CREATED("Created", "Shipment has been created but not yet dispatched"),
    IN_TRANSIT("In Transit", "Shipment is currently being transported"),
    DELIVERED("Delivered", "Shipment has been successfully delivered"),
    FAILED_DELIVERY("Failed Delivery", "Delivery attempt failed"),
    RETURNED("Returned", "Shipment returned to sender"),
    LOST("Lost", "Shipment lost during transit");
    
    private final String displayName;
    private final String description;
    
    ShipmentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminal() {
        return this == DELIVERED || this == RETURNED || this == LOST;
    }
    
    public boolean canTransitionTo(ShipmentStatus newStatus) {
        return switch (this) {
            case CREATED -> newStatus == IN_TRANSIT;
            case IN_TRANSIT -> newStatus == DELIVERED || newStatus == FAILED_DELIVERY || newStatus == LOST;
            case FAILED_DELIVERY -> newStatus == IN_TRANSIT || newStatus == RETURNED;
            default -> false; // Terminal states cannot transition
        };
    }
}
```

## Entities

### TrackingEvent (Task 14)
```java
package com.example.shipment.domain.model.entity;

import com.example.shipment.domain.model.valueobject.Location;
import java.time.Instant;
import java.util.Objects;

public class TrackingEvent {
    private final Instant timestamp;
    private final String statusDescription;
    private final Location location;
    
    public TrackingEvent(Instant timestamp, String statusDescription, Location location) {
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.statusDescription = Objects.requireNonNull(statusDescription, "Status description cannot be null");
        this.location = location; // Can be null for some events
        
        if (statusDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("Status description cannot be empty");
        }
        
        if (timestamp.isAfter(Instant.now().plusSeconds(60))) {
            throw new IllegalArgumentException("Tracking event cannot be in the future");
        }
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getStatusDescription() {
        return statusDescription;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public boolean isAfter(TrackingEvent other) {
        return this.timestamp.isAfter(other.timestamp);
    }
    
    public boolean isDeliveryEvent() {
        String lowerDescription = statusDescription.toLowerCase();
        return lowerDescription.contains("delivered") || 
               lowerDescription.contains("delivery completed");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackingEvent that = (TrackingEvent) o;
        return Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(statusDescription, that.statusDescription) &&
               Objects.equals(location, that.location);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(timestamp, statusDescription, location);
    }
}
```

## Aggregate Root

### Shipment Aggregate (Tasks 15-16)
```java
package com.example.shipment.domain.model.aggregate;

import com.example.shipment.domain.model.entity.TrackingEvent;
import com.example.shipment.domain.model.valueobject.*;
import com.example.shipment.domain.model.exception.InvalidShipmentStateException;
import java.time.Instant;
import java.util.*;

public class Shipment {
    private final ShipmentId shipmentId;
    private final OrderId orderId;
    private ShipmentStatus status;
    private final CarrierName carrierName;
    private final TrackingNumber trackingNumber;
    private final List<TrackingEvent> trackingHistory;
    private final Instant createdAt;
    private Instant dispatchedAt;
    private Instant deliveredAt;
    
    // Constructor for creating new shipment (Task 16)
    public Shipment(OrderId orderId, Package packageInfo, CarrierInfo carrierInfo) {
        this.shipmentId = ShipmentId.generate();
        this.orderId = Objects.requireNonNull(orderId, "Order ID cannot be null");
        this.carrierName = Objects.requireNonNull(carrierInfo.carrierName(), "Carrier name cannot be null");
        this.trackingNumber = Objects.requireNonNull(carrierInfo.trackingNumber(), "Tracking number cannot be null");
        this.status = ShipmentStatus.CREATED;
        this.trackingHistory = new ArrayList<>();
        this.createdAt = Instant.now();
        
        // Business invariant: shipment must be dispatched immediately after creation
        dispatch();
    }
    
    // Reconstruction constructor for loading from persistence
    public Shipment(ShipmentId shipmentId, OrderId orderId, ShipmentStatus status,
                   CarrierName carrierName, TrackingNumber trackingNumber,
                   List<TrackingEvent> trackingHistory, Instant createdAt,
                   Instant dispatchedAt, Instant deliveredAt) {
        this.shipmentId = shipmentId;
        this.orderId = orderId;
        this.status = status;
        this.carrierName = carrierName;
        this.trackingNumber = trackingNumber;
        this.trackingHistory = new ArrayList<>(trackingHistory);
        this.createdAt = createdAt;
        this.dispatchedAt = dispatchedAt;
        this.deliveredAt = deliveredAt;
    }
    
    // Business method (Task 15)
    public void addTrackingEvent(TrackingEvent event) {
        Objects.requireNonNull(event, "Tracking event cannot be null");
        
        // Business rule: event timestamp must be after the last event
        if (!trackingHistory.isEmpty()) {
            TrackingEvent lastEvent = trackingHistory.get(trackingHistory.size() - 1);
            if (!event.isAfter(lastEvent)) {
                throw new InvalidShipmentStateException(
                    "New tracking event must be after the last event");
            }
        }
        
        // Business rule: cannot add events to terminal shipments
        if (status.isTerminal()) {
            throw new InvalidShipmentStateException(
                "Cannot add tracking events to " + status + " shipment");
        }
        
        trackingHistory.add(event);
        
        // Automatically update status if delivery event
        if (event.isDeliveryEvent() && status == ShipmentStatus.IN_TRANSIT) {
            markAsDelivered();
        }
    }
    
    // Business method (Task 15)
    public void markAsDelivered() {
        if (status != ShipmentStatus.IN_TRANSIT) {
            throw new InvalidShipmentStateException(
                "Can only mark in-transit shipments as delivered");
        }
        
        this.status = ShipmentStatus.DELIVERED;
        this.deliveredAt = Instant.now();
        
        // Domain event would be raised here
        // DomainEventPublisher.publish(new ShipmentDeliveredEvent(this));
    }
    
    private void dispatch() {
        if (status != ShipmentStatus.CREATED) {
            throw new InvalidShipmentStateException("Only created shipments can be dispatched");
        }
        
        this.status = ShipmentStatus.IN_TRANSIT;
        this.dispatchedAt = Instant.now();
        
        // Add initial tracking event
        TrackingEvent dispatchEvent = new TrackingEvent(
            dispatchedAt,
            "Package dispatched from fulfillment center",
            null
        );
        trackingHistory.add(dispatchEvent);
        
        // Domain event would be raised here
        // DomainEventPublisher.publish(new ShipmentDispatchedEvent(this));
    }
    
    // Getters
    public ShipmentId getShipmentId() { return shipmentId; }
    public OrderId getOrderId() { return orderId; }
    public ShipmentStatus getStatus() { return status; }
    public CarrierName getCarrierName() { return carrierName; }
    public TrackingNumber getTrackingNumber() { return trackingNumber; }
    public List<TrackingEvent> getTrackingHistory() { return Collections.unmodifiableList(trackingHistory); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    
    public boolean isInTransit() {
        return status == ShipmentStatus.IN_TRANSIT;
    }
    
    public boolean isDelivered() {
        return status == ShipmentStatus.DELIVERED;
    }
    
    public Optional<TrackingEvent> getLatestTrackingEvent() {
        return trackingHistory.isEmpty() ? 
            Optional.empty() : 
            Optional.of(trackingHistory.get(trackingHistory.size() - 1));
    }
}
```

## Repository Interface (Task 17)
```java
package com.example.shipment.domain.repository;

import com.example.shipment.domain.model.aggregate.Shipment;
import com.example.shipment.domain.model.valueobject.OrderId;
import com.example.shipment.domain.model.valueobject.ShipmentId;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository {
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(ShipmentId shipmentId);
    Optional<Shipment> findByOrderId(OrderId orderId);
    List<Shipment> findAllInTransit();
    List<Shipment> findAll();
    void delete(ShipmentId shipmentId);
    boolean existsById(ShipmentId shipmentId);
}
```

## Port Interfaces (Tasks 18-21)

### ICarrierAdapter (Task 18)
```java
package com.example.shipment.domain.port;

import com.example.shipment.domain.model.valueobject.*;
import java.util.Optional;

public interface ICarrierAdapter {
    /**
     * Creates a shipment with the carrier and returns carrier info
     */
    CarrierInfo createShipment(Package packageInfo) throws CarrierException;
    
    /**
     * Gets the latest tracking status for a shipment
     */
    Optional<TrackingUpdate> getTrackingStatus(TrackingNumber trackingNumber) throws CarrierException;
    
    /**
     * Returns the carrier name this adapter handles
     */
    CarrierName getCarrierName();
    
    /**
     * Estimates shipping cost for a package
     */
    ShippingCost estimateShippingCost(Package packageInfo) throws CarrierException;
}
```

### Supporting Value Objects (Tasks 19-20)

```java
// CarrierInfo (Task 19)
package com.example.shipment.domain.model.valueobject;

public record CarrierInfo(
    TrackingNumber trackingNumber,
    byte[] labelData,
    CarrierName carrierName
) {
    public CarrierInfo {
        Objects.requireNonNull(trackingNumber, "Tracking number cannot be null");
        Objects.requireNonNull(labelData, "Label data cannot be null");
        Objects.requireNonNull(carrierName, "Carrier name cannot be null");
        
        if (labelData.length == 0) {
            throw new IllegalArgumentException("Label data cannot be empty");
        }
    }
}

// TrackingUpdate (Task 20)
package com.example.shipment.domain.model.valueobject;

public record TrackingUpdate(
    TrackingEvent latestEvent,
    boolean isDelivered,
    List<TrackingEvent> newEvents
) {
    public TrackingUpdate {
        Objects.requireNonNull(latestEvent, "Latest event cannot be null");
        Objects.requireNonNull(newEvents, "New events list cannot be null");
    }
}

// ShippingCost
package com.example.shipment.domain.model.valueobject;

public record ShippingCost(
    BigDecimal amount,
    String currency,
    int estimatedDeliveryDays
) {
    public ShippingCost {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        
        if (estimatedDeliveryDays < 1) {
            throw new IllegalArgumentException("Delivery days must be positive");
        }
    }
}
```

### CarrierException (Task 21)
```java
package com.example.shipment.domain.model.exception;

public class CarrierException extends RuntimeException {
    private final String carrierName;
    private final String errorCode;
    
    public CarrierException(String message, String carrierName) {
        super(message);
        this.carrierName = carrierName;
        this.errorCode = null;
    }
    
    public CarrierException(String message, String carrierName, String errorCode, Throwable cause) {
        super(message, cause);
        this.carrierName = carrierName;
        this.errorCode = errorCode;
    }
    
    public String getCarrierName() {
        return carrierName;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
```

## Domain Services (Tasks 22-24)

### CarrierSelectionService (Task 22)
```java
package com.example.shipment.domain.service;

import com.example.shipment.domain.model.valueobject.*;
import com.example.shipment.domain.port.ICarrierAdapter;
import java.util.List;
import java.util.Optional;

@DomainService
public class CarrierSelectionService {
    private final List<ICarrierAdapter> availableCarriers;
    private final CarrierSelectionStrategy selectionStrategy;
    
    public CarrierSelectionService(List<ICarrierAdapter> availableCarriers,
                                 CarrierSelectionStrategy selectionStrategy) {
        this.availableCarriers = availableCarriers;
        this.selectionStrategy = selectionStrategy;
    }
    
    public CarrierSelectionResult selectBestCarrier(Package packageInfo) {
        if (availableCarriers.isEmpty()) {
            throw new IllegalStateException("No carriers available for selection");
        }
        
        return selectionStrategy.selectCarrier(packageInfo, availableCarriers);
    }
}
```

### CarrierSelectionStrategy (Task 23)
```java
package com.example.shipment.domain.service;

import com.example.shipment.domain.model.valueobject.*;
import com.example.shipment.domain.port.ICarrierAdapter;
import java.util.List;

public interface CarrierSelectionStrategy {
    CarrierSelectionResult selectCarrier(Package packageInfo, List<ICarrierAdapter> availableCarriers);
}

// Result object
public record CarrierSelectionResult(
    ICarrierAdapter selectedCarrier,
    ShippingCost estimatedCost,
    String selectionReason
) {}
```

### CostEffectiveCarrierStrategy (Task 24)
```java
package com.example.shipment.domain.service;

import com.example.shipment.domain.model.valueobject.*;
import com.example.shipment.domain.port.ICarrierAdapter;
import com.example.shipment.domain.model.exception.CarrierException;
import java.util.List;
import java.util.Optional;

@Component
public class CostEffectiveCarrierStrategy implements CarrierSelectionStrategy {
    
    @Override
    public CarrierSelectionResult selectCarrier(Package packageInfo, List<ICarrierAdapter> availableCarriers) {
        ICarrierAdapter bestCarrier = null;
        ShippingCost lowestCost = null;
        StringBuilder reasons = new StringBuilder();
        
        for (ICarrierAdapter carrier : availableCarriers) {
            try {
                ShippingCost cost = carrier.estimateShippingCost(packageInfo);
                
                if (lowestCost == null || cost.amount().compareTo(lowestCost.amount()) < 0) {
                    bestCarrier = carrier;
                    lowestCost = cost;
                    reasons.append("Selected ").append(carrier.getCarrierName())
                          .append(" for lowest cost: ").append(cost.amount())
                          .append(" ").append(cost.currency());
                }
                
                // Business rule: prefer faster delivery if cost difference is < 10%
                if (lowestCost != null && cost.estimatedDeliveryDays() < lowestCost.estimatedDeliveryDays()) {
                    BigDecimal costDifference = cost.amount().subtract(lowestCost.amount())
                                                           .abs()
                                                           .divide(lowestCost.amount(), 2, RoundingMode.HALF_UP);
                    
                    if (costDifference.compareTo(new BigDecimal("0.10")) <= 0) {
                        bestCarrier = carrier;
                        lowestCost = cost;
                        reasons.append(" Upgraded to faster delivery with <10% cost difference");
                    }
                }
                
            } catch (CarrierException e) {
                reasons.append("Skipped ").append(carrier.getCarrierName())
                      .append(" due to error: ").append(e.getMessage()).append("; ");
            }
        }
        
        if (bestCarrier == null) {
            throw new IllegalStateException("No carriers available for package");
        }
        
        return new CarrierSelectionResult(bestCarrier, lowestCost, reasons.toString());
    }
}
```

This implementation guide covers all domain layer tasks (5-24) with complete, production-ready code examples following DDD principles and the hexagonal architecture pattern.