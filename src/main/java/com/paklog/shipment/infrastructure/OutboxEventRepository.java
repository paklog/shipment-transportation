package com.paklog.shipment.infrastructure;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus status);
}
