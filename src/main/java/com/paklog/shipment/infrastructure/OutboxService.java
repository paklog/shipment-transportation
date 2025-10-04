package com.paklog.shipment.infrastructure;

import com.paklog.shipment.config.OutboxProperties;
import com.paklog.shipment.domain.DomainEvent;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxService {
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxProperties outboxProperties;

    public OutboxService(OutboxEventRepository outboxEventRepository, OutboxProperties outboxProperties) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxProperties = outboxProperties;
    }

    @Transactional
    public OutboxEvent save(DomainEvent event) {
        return outboxEventRepository.save(new OutboxEvent(event));
    }

    public List<OutboxEvent> getPendingEvents() {
        return outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxEvent.EventStatus.PENDING);
    }

    @Transactional
    public void markEventAsProcessed(String eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.markProcessed();
            outboxEventRepository.save(event);
        });
    }

    @Transactional
    public void markEventAsFailed(String eventId, String error) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.markForRetry(error);
            if (event.getAttemptCount() >= outboxProperties.getMaxAttempts()) {
                event.markFailed(error);
            }
            outboxEventRepository.save(event);
        });
    }
}
