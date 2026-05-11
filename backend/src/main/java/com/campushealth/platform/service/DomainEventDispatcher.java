package com.campushealth.platform.service;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campushealth.platform.entity.DomainOutboxEventEntity;
import com.campushealth.platform.entity.DomainOutboxEventEntity.EventStatus;
import com.campushealth.platform.repository.DomainOutboxEventRepository;

@Service
public class DomainEventDispatcher {

    private final DomainOutboxEventRepository domainOutboxEventRepository;

    public DomainEventDispatcher(DomainOutboxEventRepository domainOutboxEventRepository) {
        this.domainOutboxEventRepository = domainOutboxEventRepository;
    }

    @Scheduled(fixedDelayString = "${campus.platform.outbox.dispatch-interval-ms:1000}")
    @Transactional
    public void dispatchPendingEvents() {
        List<DomainOutboxEventEntity> events = domainOutboxEventRepository.findTop50ByStatusOrderByCreatedAtAscIdAsc(EventStatus.PENDING);
        for (DomainOutboxEventEntity event : events) {
            event.markDispatched(Instant.now());
        }
        domainOutboxEventRepository.saveAll(events);
    }
}