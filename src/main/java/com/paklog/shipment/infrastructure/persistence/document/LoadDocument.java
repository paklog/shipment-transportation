package com.paklog.shipment.infrastructure.persistence.document;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.LoadStatus;
import com.paklog.shipment.domain.ShipmentId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Document(collection = "loads")
public class LoadDocument {

    @Id
    private String id;
    private String status;
    private List<String> shipmentIds;
    private String carrierName;
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;

    public static LoadDocument fromDomain(Load load) {
        LoadDocument doc = new LoadDocument();
        doc.setId(load.getId().toString());
        doc.setStatus(load.getStatus().name());
        doc.setShipmentIds(load.getShipmentIds().stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
        doc.setCarrierName(load.getCarrierName() != null ? load.getCarrierName().name() : null);
        doc.setTotalWeight(load.getTotalWeight());
        doc.setTotalVolume(load.getTotalVolume());
        return doc;
    }

    public Load toDomain() {
        List<ShipmentId> shipmentIdList = shipmentIds == null
            ? List.of()
            : shipmentIds.stream().map(ShipmentId::of).collect(Collectors.toList());

        CarrierName carrier = carrierName != null ? CarrierName.valueOf(carrierName) : null;

        return Load.restore(
            LoadId.of(this.id),
            LoadStatus.valueOf(this.status),
            carrier,
            shipmentIdList,
            totalWeight,
            totalVolume
        );
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getShipmentIds() {
        return shipmentIds;
    }

    public void setShipmentIds(List<String> shipmentIds) {
        this.shipmentIds = shipmentIds;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public BigDecimal getTotalWeight() {
        return totalWeight;
    }

    public void setTotalWeight(BigDecimal totalWeight) {
        this.totalWeight = totalWeight;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(BigDecimal totalVolume) {
        this.totalVolume = totalVolume;
    }
}
