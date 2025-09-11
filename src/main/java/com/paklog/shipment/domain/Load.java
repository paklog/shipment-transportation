package com.paklog.shipment.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Load {

    private final LoadId id;
    private LoadStatus status;
    private CarrierName carrierName;
    private final List<ShipmentId> shipmentIds;

    // Properties for planning
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;

    public Load(LoadId id) {
        this.id = Objects.requireNonNull(id);
        this.status = LoadStatus.OPEN;
        this.shipmentIds = new ArrayList<>();
        this.totalWeight = BigDecimal.ZERO;
        this.totalVolume = BigDecimal.ZERO;
    }

    public void addShipment(Shipment shipment) {
        if (this.status != LoadStatus.OPEN) {
            throw new IllegalStateException("Cannot add shipments to a load that is not in OPEN state.");
        }
        if (shipmentIds.contains(shipment.getId())) {
            throw new IllegalArgumentException("Shipment is already in this load.");
        }
        this.shipmentIds.add(shipment.getId());
        // In a real scenario, you would get weight/volume from the shipment
        // For now, we'll just increment dummy values
        this.totalWeight = this.totalWeight.add(BigDecimal.ONE); // Placeholder
        this.totalVolume = this.totalVolume.add(BigDecimal.ONE); // Placeholder
    }

    public void assignCarrier(CarrierName carrierName) {
        if (this.status != LoadStatus.OPEN) {
            throw new IllegalStateException("Cannot assign a carrier to a load that is not in OPEN state.");
        }
        this.carrierName = carrierName;
    }

    public void book() {
        if (this.status != LoadStatus.OPEN || this.carrierName == null) {
            throw new IllegalStateException("Load must be in OPEN state and have a carrier to be booked.");
        }
        this.status = LoadStatus.BOOKED;
    }

    public void tender() {
        if (this.status != LoadStatus.OPEN) {
            throw new IllegalStateException("Can only tender an OPEN load.");
        }
        this.status = LoadStatus.TENDERED;
    }

    public void reopen() {
        if (this.status != LoadStatus.TENDERED) {
            throw new IllegalStateException("Can only reopen a TENDERED load.");
        }
        this.status = LoadStatus.OPEN;
    }

    public void ship() {
        if (this.status != LoadStatus.BOOKED) {
            throw new IllegalStateException("Can only ship a BOOKED load.");
        }
        this.status = LoadStatus.SHIPPED;
    }

    // Getters
    public LoadId getId() {
        return id;
    }

    public LoadStatus getStatus() {
        return status;
    }

    public CarrierName getCarrierName() {
        return carrierName;
    }

    public List<ShipmentId> getShipmentIds() {
        return Collections.unmodifiableList(shipmentIds);
    }

    public BigDecimal getTotalWeight() {
        return totalWeight;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }
}
