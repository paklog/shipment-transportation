package com.paklog.shipment.infrastructure.api.mapper;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadStatus;
import com.paklog.shipment.domain.Location;
import com.paklog.shipment.domain.Pickup;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShippingCost;
import com.paklog.shipment.domain.Tender;
import com.paklog.shipment.infrastructure.api.gen.dto.LoadCollection;
import com.paklog.shipment.infrastructure.api.gen.dto.PickupDetails;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class LoadMapper {

    public com.paklog.shipment.infrastructure.api.gen.dto.Load toDto(Load load) {
        Objects.requireNonNull(load, "load");

        var dto = new com.paklog.shipment.infrastructure.api.gen.dto.Load();
        dto.setId(load.getId().getValue());
        dto.setReference(load.getReference());
        dto.setStatus(toDto(load.getStatus()));
        dto.setCarrierName(toDto(load.getCarrierName()));
        dto.setShipments(toShipmentUuidSet(load.getShipmentIds()));
        dto.setOrigin(toDto(load.getOrigin()));
        dto.setDestination(toDto(load.getDestination()));
        dto.setRequestedPickupDate(load.getRequestedPickupDate());
        dto.setRequestedDeliveryDate(load.getRequestedDeliveryDate());
        dto.setPickup(toDto(load.getPickup()));
        dto.setTender(toDto(load.getTender()));
        dto.setNotes(load.getNotes());
        dto.setCreatedAt(load.getCreatedAt());
        dto.setUpdatedAt(load.getUpdatedAt());
        return dto;
    }

    public LoadCollection toDto(Page<Load> page) {
        var collection = new LoadCollection();
        collection.setItems(page.getContent().stream().map(this::toDto).toList());
        collection.setPage(page.getNumber());
        collection.setSize(page.getSize());
        collection.setTotalItems(Math.toIntExact(page.getTotalElements()));
        collection.setTotalPages(page.getTotalPages());
        return collection;
    }

    public LoadStatus toDomain(com.paklog.shipment.infrastructure.api.gen.dto.LoadStatus status) {
        return status != null ? LoadStatus.valueOf(status.name()) : null;
    }

    public CarrierName toDomain(com.paklog.shipment.infrastructure.api.gen.dto.CarrierName carrierName) {
        return carrierName != null ? CarrierName.valueOf(carrierName.name()) : null;
    }

    public Location toDomain(com.paklog.shipment.infrastructure.api.gen.dto.Location location) {
        if (location == null) {
            return null;
        }
        return new Location(
                location.getName(),
                location.getAddressLine1(),
                location.getAddressLine2(),
                location.getCity(),
                location.getStateOrProvince(),
                location.getPostalCode(),
                location.getCountry()
        );
    }

    public com.paklog.shipment.infrastructure.api.gen.dto.Location toDto(Location location) {
        if (location == null) {
            return null;
        }
        var dto = new com.paklog.shipment.infrastructure.api.gen.dto.Location();
        dto.setName(location.name());
        dto.setAddressLine1(location.addressLine1());
        dto.setAddressLine2(location.addressLine2());
        dto.setCity(location.city());
        dto.setStateOrProvince(location.stateOrProvince());
        dto.setPostalCode(location.postalCode());
        dto.setCountry(location.country());
        return dto;
    }

    public Set<UUID> toShipmentIdSet(Collection<UUID> shipmentIds) {
        if (shipmentIds == null) {
            return Set.of();
        }
        return shipmentIds.stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public com.paklog.shipment.infrastructure.api.gen.dto.CarrierName toDto(CarrierName carrierName) {
        return carrierName != null ? com.paklog.shipment.infrastructure.api.gen.dto.CarrierName.valueOf(carrierName.name()) : null;
    }

    public com.paklog.shipment.infrastructure.api.gen.dto.LoadStatus toDto(LoadStatus status) {
        return status != null ? com.paklog.shipment.infrastructure.api.gen.dto.LoadStatus.valueOf(status.name()) : null;
    }

    public PickupDetails toDto(Pickup pickup) {
        if (pickup == null) {
            return null;
        }
        var dto = new PickupDetails();
        dto.setConfirmationNumber(pickup.confirmationNumber());
        dto.setScheduledFor(pickup.scheduledFor());
        dto.setLocation(toDto(pickup.location()));
        dto.setContactName(pickup.contactName());
        dto.setContactPhone(pickup.contactPhone());
        dto.setInstructions(pickup.instructions());
        return dto;
    }

    public com.paklog.shipment.infrastructure.api.gen.dto.Tender toDto(Tender tender) {
        if (tender == null) {
            return null;
        }
        var dto = new com.paklog.shipment.infrastructure.api.gen.dto.Tender();
        dto.setStatus(toDto(tender.status()));
        dto.setExpiresAt(tender.expiresAt());
        dto.setRespondedAt(tender.respondedAt());
        dto.setRespondedBy(tender.respondedBy());
        if (tender.decision() != null) {
            dto.setDecision(com.paklog.shipment.infrastructure.api.gen.dto.Tender.DecisionEnum.valueOf(tender.decision().name()));
        }
        dto.setNotes(tender.notes());
        return dto;
    }

    public com.paklog.shipment.infrastructure.api.gen.dto.TenderStatus toDto(com.paklog.shipment.domain.TenderStatus status) {
        return status != null ? com.paklog.shipment.infrastructure.api.gen.dto.TenderStatus.valueOf(status.name()) : null;
    }

    public com.paklog.shipment.infrastructure.api.gen.dto.ShippingCost toDto(ShippingCost shippingCost) {
        if (shippingCost == null) {
            return null;
        }
        var dto = new com.paklog.shipment.infrastructure.api.gen.dto.ShippingCost();
        dto.setAmount(shippingCost.amount().doubleValue());
        dto.setCurrency(shippingCost.currency());
        dto.setEstimatedDeliveryDays(shippingCost.estimatedDeliveryDays());
        return dto;
    }

    private LinkedHashSet<UUID> toShipmentUuidSet(Set<ShipmentId> shipmentIds) {
        return shipmentIds.stream()
                .map(ShipmentId::getValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
