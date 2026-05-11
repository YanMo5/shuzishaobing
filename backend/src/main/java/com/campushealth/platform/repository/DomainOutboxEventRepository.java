package com.campushealth.platform.repository;

import com.campushealth.platform.entity.DomainOutboxEventEntity;
import com.campushealth.platform.entity.DomainOutboxEventEntity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DomainOutboxEventRepository extends JpaRepository<DomainOutboxEventEntity, Long> {

    List<DomainOutboxEventEntity> findTop50ByStatusOrderByCreatedAtAscIdAsc(EventStatus status);
}