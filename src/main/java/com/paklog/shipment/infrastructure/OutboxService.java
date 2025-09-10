package com.paklog.shipment.infrastructure;

import com.paklog.shipment.domain.DomainEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxService {
    private final OutboxEventRepository outboxEventRepository;

    public OutboxService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public OutboxEvent save(DomainEvent event) {
        OutboxEvent outboxEvent = new OutboxEvent(
            event.getAggregateId(),
            event.getAggregateType(),
            event.getEventType(),
            event.getPayload()
        );
        return outboxEventRepository.save(outboxEvent);
    }

    public List<OutboxEvent> getPendingEvents() {
        return outboxEventRepository.findByStatus(OutboxEvent.EventStatus.PENDING);
    }

    @Transactional
    public void markEventAsProcessed(String eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setProcessed(true);
            outboxEventRepository.save(event);
        });
    }

    @Transactional
    public void markEventAsFailed(String eventId, String error) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.setProcessed(false);
            event.setErrorMessage(error);
            outboxEventRepository.save(event);
        });
    }
}