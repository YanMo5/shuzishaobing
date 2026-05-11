package com.campushealth.platform.service;

import com.campushealth.platform.entity.DomainOutboxEventEntity;
import com.campushealth.platform.entity.DomainOutboxEventEntity.EventStatus;
import com.campushealth.platform.repository.DomainOutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class DomainEventOutboxService {

    private final DomainOutboxEventRepository domainOutboxEventRepository;

    public DomainEventOutboxService(DomainOutboxEventRepository domainOutboxEventRepository) {
        this.domainOutboxEventRepository = domainOutboxEventRepository;
    }

    @Transactional
    public DomainOutboxEventEntity enqueue(String eventType, String aggregateType, String aggregateId, String payload) {
        DomainOutboxEventEntity event = new DomainOutboxEventEntity(
                UUID.randomUUID().toString(),
                eventType,
                aggregateType,
                aggregateId,
                payload,
                EventStatus.PENDING,
                Instant.now()
        );
        return domainOutboxEventRepository.save(event);
    }
}