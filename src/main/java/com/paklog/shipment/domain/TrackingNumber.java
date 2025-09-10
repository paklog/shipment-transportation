package com.paklog.shipment.domain;

import java.util.Objects;

public class TrackingNumber {
    private final String value;

    public TrackingNumber(String value) {
        this.value = value;
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
