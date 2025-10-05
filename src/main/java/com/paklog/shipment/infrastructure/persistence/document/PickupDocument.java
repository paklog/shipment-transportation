package com.paklog.shipment.infrastructure.persistence.document;

import com.paklog.shipment.domain.Pickup;
import java.time.OffsetDateTime;

public class PickupDocument {
    private String confirmationNumber;
    private OffsetDateTime scheduledFor;
    private LocationDocument location;
    private String contactName;
    private String contactPhone;
    private String instructions;

    public static PickupDocument fromDomain(Pickup domain) {
        if (domain == null) return null;
        PickupDocument doc = new PickupDocument();
        doc.setConfirmationNumber(domain.confirmationNumber());
        doc.setScheduledFor(domain.scheduledFor());
        doc.setLocation(LocationDocument.fromDomain(domain.location()));
        doc.setContactName(domain.contactName());
        doc.setContactPhone(domain.contactPhone());
        doc.setInstructions(domain.instructions());
        return doc;
    }

    public Pickup toDomain() {
        return new Pickup(confirmationNumber, scheduledFor, location != null ? location.toDomain() : null, contactName, contactPhone, instructions);
    }

    // Getters and Setters
    public String getConfirmationNumber() { return confirmationNumber; }
    public void setConfirmationNumber(String confirmationNumber) { this.confirmationNumber = confirmationNumber; }
    public OffsetDateTime getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(OffsetDateTime scheduledFor) { this.scheduledFor = scheduledFor; }
    public LocationDocument getLocation() { return location; }
    public void setLocation(LocationDocument location) { this.location = location; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
