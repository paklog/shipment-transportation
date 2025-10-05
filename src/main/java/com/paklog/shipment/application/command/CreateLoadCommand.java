package com.paklog.shipment.application.command;

import com.paklog.shipment.domain.Location;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record CreateLoadCommand(
    String reference,
    Set<UUID> shipments,
    Location origin,
    Location destination,
    LocalDate requestedPickupDate,
    LocalDate requestedDeliveryDate,
    String notes
) {}
