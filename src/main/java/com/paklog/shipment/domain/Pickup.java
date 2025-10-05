package com.paklog.shipment.domain;

import java.time.OffsetDateTime;

public record Pickup(
    String confirmationNumber,
    OffsetDateTime scheduledFor,
    Location location,
    String contactName,
    String contactPhone,
    String instructions
) {}
