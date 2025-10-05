package com.paklog.shipment.infrastructure;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.LoadStatus;
import com.paklog.shipment.domain.repository.ILoadRepository;
import com.paklog.shipment.infrastructure.persistence.document.LoadDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        LoadDocument doc = mongoTemplate.findById(loadId.getValue().toString(), LoadDocument.class);
        return Optional.ofNullable(doc).map(LoadDocument::toDomain);
    }

    @Override
    public Page<Load> findAll(Pageable pageable, LoadStatus status, CarrierName carrierName) {
        Query query = new Query();

        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        if (carrierName != null) {
            query.addCriteria(Criteria.where("carrierName").is(carrierName));
        }

        long total = mongoTemplate.count(query, LoadDocument.class);

        List<LoadDocument> docs = mongoTemplate.find(query.with(pageable), LoadDocument.class);
        List<Load> loads = docs.stream()
                .map(LoadDocument::toDomain)
                .collect(Collectors.toList());

        return new PageImpl<>(loads, pageable, total);
    }

    @Override
    public void delete(Load load) {
        LoadDocument doc = LoadDocument.fromDomain(load);
        mongoTemplate.remove(doc);
    }
}
