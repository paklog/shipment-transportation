package com.paklog.shipment.domain;

import java.util.Objects;
import java.util.UUID;

public final class LoadId {
    private final UUID value;

    private LoadId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }

    public static LoadId of(UUID value) {
        return new LoadId(value);
    }

    public static LoadId of(String value) {
        return new LoadId(UUID.fromString(value));
    }

    public static LoadId generate() {
        return new LoadId(UUID.randomUUID());
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoadId loadId = (LoadId) o;
        return value.equals(loadId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
