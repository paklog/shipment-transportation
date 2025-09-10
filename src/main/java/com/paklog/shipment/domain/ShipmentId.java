package com.paklog.shipment.domain;

import java.util.Objects;
import java.util.UUID;

public class ShipmentId {
    private final String value;

    private ShipmentId(String value) {
        this.value = value;
    }

    public static ShipmentId of(String value) {
        return new ShipmentId(value);
    }

    public static ShipmentId newId() {
        return new ShipmentId(UUID.randomUUID().toString());
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShipmentId that = (ShipmentId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
