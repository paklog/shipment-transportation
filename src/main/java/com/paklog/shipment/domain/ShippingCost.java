package com.paklog.shipment.domain;

import java.math.BigDecimal;
import java.util.Objects;

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
