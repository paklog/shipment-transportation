package com.paklog.shipment.infrastructure;

import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.Shipment;
import com.paklog.shipment.domain.ShipmentId;
import com.paklog.shipment.domain.ShipmentStatus;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import com.paklog.shipment.infrastructure.persistence.ShipmentDocument;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ShipmentMongoRepository implements ShipmentRepository {
    private final MongoTemplate mongoTemplate;

    public ShipmentMongoRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Shipment save(Shipment shipment) {
        ShipmentDocument doc = ShipmentDocument.fromDomain(shipment);
        ShipmentDocument savedDoc = mongoTemplate.save(doc);
        return savedDoc.toDomain();
    }

    @Override
    public Optional<Shipment> findById(ShipmentId id) {
        ShipmentDocument doc = mongoTemplate.findById(id.getValue(), ShipmentDocument.class);
        return Optional.ofNullable(doc).map(ShipmentDocument::toDomain);
    }

    @Override
    public Optional<Shipment> findByOrderId(OrderId orderId) {
        Query query = new Query(Criteria.where("orderId").is(orderId.getValue()));
        ShipmentDocument doc = mongoTemplate.findOne(query, ShipmentDocument.class);
        return Optional.ofNullable(doc).map(ShipmentDocument::toDomain);
    }

    @Override
    public Optional<Shipment> findByTrackingNumber(TrackingNumber trackingNumber) {
        Query query = new Query(Criteria.where("trackingNumber").is(trackingNumber.getValue()));
        ShipmentDocument doc = mongoTemplate.findOne(query, ShipmentDocument.class);
        return Optional.ofNullable(doc).map(ShipmentDocument::toDomain);
    }

    @Override
    public List<Shipment> findPageInTransit(String lastSeenId, int limit) {
        Query query = new Query(Criteria.where("status").is(ShipmentStatus.IN_TRANSIT.name()))
                .limit(limit)
                .with(Sort.by(Sort.Direction.ASC, "id"));
        if (lastSeenId != null) {
            query.addCriteria(Criteria.where("id").gt(lastSeenId));
        }
        List<ShipmentDocument> docs = mongoTemplate.find(query, ShipmentDocument.class);
        return docs.stream().map(ShipmentDocument::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Shipment> findAll() {
        return mongoTemplate.findAll(ShipmentDocument.class).stream()
                .map(ShipmentDocument::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(ShipmentId shipmentId) {
        Query query = new Query(Criteria.where("id").is(shipmentId.getValue()));
        mongoTemplate.remove(query, ShipmentDocument.class);
    }

    @Override
    public boolean existsById(ShipmentId shipmentId) {
        Query query = new Query(Criteria.where("id").is(shipmentId.getValue()));
        return mongoTemplate.exists(query, ShipmentDocument.class);
    }

    @Override
    public List<Shipment> findByLoadId(LoadId loadId) {
        // This is a conceptual implementation. In a real system, you would have a proper relationship.
        // For now, we assume all shipments are on the unassigned load.
        if (loadId.toString().equals("00000000-0000-0000-0000-000000000000")) {
            return findAll();
        }
        return List.of();
    }

    @Override
    public List<Shipment> findAllById(List<ShipmentId> shipmentIds) {
        List<String> ids = shipmentIds.stream()
                .map(ShipmentId::getValue)
                .map(Object::toString)
                .collect(Collectors.toList());
        Query query = new Query(Criteria.where("id").in(ids));
        return mongoTemplate.find(query, ShipmentDocument.class).stream()
                .map(ShipmentDocument::toDomain)
                .collect(Collectors.toList());
    }
}
