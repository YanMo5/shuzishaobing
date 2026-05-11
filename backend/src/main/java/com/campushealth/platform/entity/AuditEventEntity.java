package com.campushealth.platform.entity;

import com.campushealth.platform.model.AuditActionType;
import com.campushealth.platform.model.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditActionType actionType;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    private String outcome;

    @Column(length = 1000)
    private String details;

    protected AuditEventEntity() {
    }

    public AuditEventEntity(String eventId, Instant timestamp, String actorUserId, UserRole actorRole, AuditActionType actionType, String resource, String outcome, String details) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.actorUserId = actorUserId;
        this.actorRole = actorRole;
        this.actionType = actionType;
        this.resource = resource;
        this.outcome = outcome;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public UserRole getActorRole() {
        return actorRole;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public String getResource() {
        return resource;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getDetails() {
        return details;
    }
}