package com.campushealth.platform.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campushealth.platform.entity.AuditEventEntity;
import com.campushealth.platform.model.AuditActionType;
import com.campushealth.platform.model.AuditEvent;
import com.campushealth.platform.model.CampusPrincipal;
import com.campushealth.platform.model.UserRole;
import com.campushealth.platform.repository.AuditEventRepository;

@Service
public class AuditTrailService {

    private final AuditEventRepository auditEventRepository;
    private final DomainEventOutboxService domainEventOutboxService;

    public AuditTrailService(AuditEventRepository auditEventRepository, DomainEventOutboxService domainEventOutboxService) {
        this.auditEventRepository = auditEventRepository;
        this.domainEventOutboxService = domainEventOutboxService;
    }

    @Transactional
    public AuditEvent record(CampusPrincipal principal, AuditActionType actionType, String resource, String outcome, String details) {
        CampusPrincipal actor = principal == null
                ? new CampusPrincipal("anonymous", "匿名用户", UserRole.STUDENT, null)
                : principal;
        AuditEvent event = new AuditEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                actor.userId(),
                actor.role(),
                actionType,
                resource,
                outcome,
                details
        );
            auditEventRepository.save(new AuditEventEntity(
                event.eventId(),
                event.timestamp(),
                event.actorUserId(),
                event.actorRole(),
                event.actionType(),
                event.resource(),
                event.outcome(),
                event.details()
            ));
            domainEventOutboxService.enqueue(
                "AuditEventRecorded",
                "AuditEvent",
                event.eventId(),
                "{"
                    + "\"eventId\":\"" + event.eventId() + "\"," 
                    + "\"actionType\":\"" + event.actionType() + "\"," 
                    + "\"resource\":\"" + event.resource() + "\"," 
                    + "\"outcome\":\"" + event.outcome() + "\""
                    + "}"
            );
        return event;
    }

    public List<AuditEvent> listEvents() {
            return auditEventRepository.findAll().stream()
                .map(entity -> new AuditEvent(
                    entity.getEventId(),
                    entity.getTimestamp(),
                    entity.getActorUserId(),
                    entity.getActorRole(),
                    entity.getActionType(),
                    entity.getResource(),
                    entity.getOutcome(),
                    entity.getDetails()
                ))
                .toList();
    }
}