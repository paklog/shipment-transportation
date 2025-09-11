package com.paklog.shipment.infrastructure;

import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.repository.ILoadRepository;
import com.paklog.shipment.infrastructure.persistence.document.LoadDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class LoadMongoRepository implements ILoadRepository {

    private final MongoTemplate mongoTemplate;

    public LoadMongoRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(Load load) {
        LoadDocument doc = LoadDocument.fromDomain(load);
        mongoTemplate.save(doc);
    }

    @Override
    public Optional<Load> findById(LoadId loadId) {
        LoadDocument doc = mongoTemplate.findById(loadId.toString(), LoadDocument.class);
        return Optional.ofNullable(doc).map(LoadDocument::toDomain);
    }
}
