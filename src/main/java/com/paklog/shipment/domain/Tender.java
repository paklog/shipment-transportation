package com.paklog.shipment.domain;

import java.time.OffsetDateTime;

public record Tender(
    TenderStatus status,
    OffsetDateTime expiresAt,
    OffsetDateTime respondedAt,
    String respondedBy,
    Decision decision,
    String notes
) {
    public enum Decision {
        ACCEPTED,
        DECLINED
    }

    public static Tender notTendered() {
        return new Tender(TenderStatus.NOT_TENDERED, null, null, null, null, null);
    }
}
