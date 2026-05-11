package com.campushealth.platform.model;

import java.time.Instant;

public record AuditEvent(
        String eventId,
        Instant timestamp,
        String actorUserId,
        UserRole actorRole,
        AuditActionType actionType,
        String resource,
        String outcome,
        String details
) {
}