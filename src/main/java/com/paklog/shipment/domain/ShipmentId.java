package com.paklog.shipment.domain;

import java.util.Objects;
import java.util.UUID;

public final class ShipmentId {
    private final UUID value;

    private ShipmentId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    public static ShipmentId of(String value) {
        return new ShipmentId(UUID.fromString(value));
    }

    public static ShipmentId generate() {
        return new ShipmentId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}