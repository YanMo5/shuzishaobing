package com.campushealth.platform.entity;

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
@Table(name = "domain_outbox_events")
public class DomainOutboxEventEntity {

    public enum EventStatus {
        PENDING,
        DISPATCHED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(nullable = false, length = 60)
    private String eventType;

    @Column(nullable = false, length = 60)
    private String aggregateType;

    @Column(nullable = false, length = 60)
    private String aggregateId;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant dispatchedAt;

    protected DomainOutboxEventEntity() {
    }

    public DomainOutboxEventEntity(String eventId, String eventType, String aggregateType, String aggregateId, String payload, EventStatus status, Instant createdAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public EventStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDispatchedAt() {
        return dispatchedAt;
    }

    public void markDispatched(Instant dispatchedAt) {
        this.status = EventStatus.DISPATCHED;
        this.dispatchedAt = dispatchedAt;
    }
}