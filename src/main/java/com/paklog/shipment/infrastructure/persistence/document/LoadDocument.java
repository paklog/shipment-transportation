package com.paklog.shipment.infrastructure.persistence.document;

import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.LoadStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.stream.Collectors;

@Document(collection = "loads")
public class LoadDocument {

    @Id
    private String id;
    private String status;
    private List<String> shipmentIds;

    public static LoadDocument fromDomain(Load load) {
        LoadDocument doc = new LoadDocument();
        doc.setId(load.getId().toString());
        doc.setStatus(load.getStatus().name());
        doc.setShipmentIds(load.getShipmentIds().stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
        return doc;
    }

    // This is a simplified mapping. A real implementation would need to restore state more carefully.
    public Load toDomain() {
        LoadId loadId = LoadId.of(this.id);
        Load load = new Load(loadId);
        // This is a simplification; a full implementation would need to restore
        // the state of the aggregate, not just the ID.
        return load;
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
}
