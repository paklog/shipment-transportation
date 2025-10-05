package com.paklog.shipment.domain;

import java.util.Objects;

public record Location(
    String name,
    String addressLine1,
    String addressLine2,
    String city,
    String stateOrProvince,
    String postalCode,
    String country
) {
    public Location {
        Objects.requireNonNull(name);
        Objects.requireNonNull(addressLine1);
        Objects.requireNonNull(city);
        Objects.requireNonNull(stateOrProvince);
        Objects.requireNonNull(postalCode);
        Objects.requireNonNull(country);
    }
}
