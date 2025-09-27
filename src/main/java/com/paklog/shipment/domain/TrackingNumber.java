package com.paklog.shipment.domain;

import java.util.Objects;

public class TrackingNumber {
    private final String value;

    public TrackingNumber(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TrackingNumber cannot be null or blank");
        }
        String sanitized = value.trim();
        if (!sanitized.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("TrackingNumber contains invalid characters");
        }
        this.value = sanitized;
    }

    public static TrackingNumber of(String value) {
        return new TrackingNumber(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackingNumber that = (TrackingNumber) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
