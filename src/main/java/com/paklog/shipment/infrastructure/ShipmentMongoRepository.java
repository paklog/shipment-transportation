package com.paklog.shipment.infrastructure;

import com.paklog.shipment.domain.*;
import com.paklog.shipment.domain.repository.ShipmentRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
    public Optional<Shipment> findByTrackingNumber(TrackingNumber trackingNumber) {
        Query query = new Query(Criteria.where("trackingNumber").is(trackingNumber.getValue()));
        ShipmentDocument doc = mongoTemplate.findOne(query, ShipmentDocument.class);
        return Optional.ofNullable(doc).map(ShipmentDocument::toDomain);
    }
}