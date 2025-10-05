package com.paklog.shipment.application.command;

import com.paklog.shipment.domain.LoadStatus;
import java.time.LocalDate;

public record UpdateLoadCommand(
    String reference,
    LocalDate requestedPickupDate,
    LocalDate requestedDeliveryDate,
    LoadStatus status,
    String notes
) {}
