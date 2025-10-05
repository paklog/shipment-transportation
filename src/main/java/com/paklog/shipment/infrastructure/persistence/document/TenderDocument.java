package com.paklog.shipment.infrastructure.persistence.document;

import com.paklog.shipment.domain.Tender;
import com.paklog.shipment.domain.TenderStatus;
import java.time.OffsetDateTime;

public class TenderDocument {
    private TenderStatus status;
    private OffsetDateTime expiresAt;
    private OffsetDateTime respondedAt;
    private String respondedBy;
    private String decision; // Enum stored as String
    private String notes;

    public static TenderDocument fromDomain(Tender domain) {
        if (domain == null) return null;
        TenderDocument doc = new TenderDocument();
        doc.setStatus(domain.status());
        doc.setExpiresAt(domain.expiresAt());
        doc.setRespondedAt(domain.respondedAt());
        doc.setRespondedBy(domain.respondedBy());
        doc.setDecision(domain.decision() != null ? domain.decision().name() : null);
        doc.setNotes(domain.notes());
        return doc;
    }

    public Tender toDomain() {
        Tender.Decision decisionEnum = decision != null ? Tender.Decision.valueOf(decision) : null;
        return new Tender(status, expiresAt, respondedAt, respondedBy, decisionEnum, notes);
    }

    // Getters and Setters
    public TenderStatus getStatus() { return status; }
    public void setStatus(TenderStatus status) { this.status = status; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public OffsetDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(OffsetDateTime respondedAt) { this.respondedAt = respondedAt; }
    public String getRespondedBy() { return respondedBy; }
    public void setRespondedBy(String respondedBy) { this.respondedBy = respondedBy; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
