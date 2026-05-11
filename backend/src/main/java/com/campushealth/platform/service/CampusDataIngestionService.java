package com.campushealth.platform.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campushealth.platform.dto.CampusHealthSignalRequest;
import com.campushealth.platform.entity.CampusHealthSignalEntity;
import com.campushealth.platform.model.CampusHealthSignal;
import com.campushealth.platform.model.CampusPrincipal;
import com.campushealth.platform.repository.CampusHealthSignalRepository;

@Service
public class CampusDataIngestionService {

    private final CampusHealthSignalRepository campusHealthSignalRepository;
    private final DomainEventOutboxService domainEventOutboxService;

    public CampusDataIngestionService(CampusHealthSignalRepository campusHealthSignalRepository,
                                      DomainEventOutboxService domainEventOutboxService) {
        this.campusHealthSignalRepository = campusHealthSignalRepository;
        this.domainEventOutboxService = domainEventOutboxService;
    }

    @Transactional
    public CampusHealthSignal ingest(String studentId, CampusHealthSignalRequest request, CampusPrincipal principal) {
        CampusHealthSignalEntity entity = new CampusHealthSignalEntity(
                studentId,
                request.sourceType(),
                request.sleepHours(),
                request.lateNightCountPerWeek(),
                request.nutritionScore(),
                request.stressScore(),
                request.physicalActivityMinutesPerWeek(),
                request.infectionContacts(),
                request.feverReported(),
                request.coughReported(),
                Instant.now(),
                buildNote(request.note(), principal)
        );
        CampusHealthSignalEntity saved = campusHealthSignalRepository.save(entity);
        domainEventOutboxService.enqueue(
                "CampusHealthSignalCreated",
                "CampusHealthSignal",
                studentId,
                buildSignalPayload(saved)
        );
        return toSignal(saved);
    }

    public Optional<CampusHealthSignal> latestSignal(String studentId) {
        return campusHealthSignalRepository.findFirstByStudentIdOrderByObservedAtDescIdDesc(studentId)
                .map(this::toSignal);
    }

    public List<CampusHealthSignal> getSignals(String studentId) {
        return campusHealthSignalRepository.findByStudentIdOrderByObservedAtDescIdDesc(studentId)
                .stream()
                .map(this::toSignal)
                .toList();
    }

    private String buildNote(String note, CampusPrincipal principal) {
        String base = note == null || note.isBlank() ? "" : note.trim();
        String source = principal == null ? "" : "submitted-by=" + principal.userId();
        if (base.isEmpty()) {
            return source;
        }
        if (source.isEmpty()) {
            return base;
        }
        return base + " | " + source;
    }

    private CampusHealthSignal toSignal(CampusHealthSignalEntity entity) {
        return new CampusHealthSignal(
                entity.getStudentId(),
                entity.getSourceType(),
                entity.getSleepHours(),
                entity.getLateNightCountPerWeek(),
                entity.getNutritionScore(),
                entity.getStressScore(),
                entity.getPhysicalActivityMinutesPerWeek(),
                entity.getInfectionContacts(),
                entity.isFeverReported(),
                entity.isCoughReported(),
                entity.getObservedAt(),
                entity.getNote()
        );
    }

    private String buildSignalPayload(CampusHealthSignalEntity entity) {
        return "{"
                + "\"studentId\":\"" + entity.getStudentId() + "\"," 
                + "\"sourceType\":\"" + entity.getSourceType() + "\"," 
                + "\"sleepHours\":" + entity.getSleepHours() + ","
                + "\"lateNightCountPerWeek\":" + entity.getLateNightCountPerWeek() + ","
                + "\"nutritionScore\":" + entity.getNutritionScore() + ","
                + "\"stressScore\":" + entity.getStressScore() + ","
                + "\"physicalActivityMinutesPerWeek\":" + entity.getPhysicalActivityMinutesPerWeek() + ","
                + "\"infectionContacts\":" + entity.getInfectionContacts() + ","
                + "\"feverReported\":" + entity.isFeverReported() + ","
                + "\"coughReported\":" + entity.isCoughReported() + ","
                + "\"observedAt\":\"" + entity.getObservedAt() + "\"," 
                + "\"note\":\"" + escapeJson(entity.getNote()) + "\""
                + "}";
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}