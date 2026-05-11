package com.campushealth.platform.repository;

import com.campushealth.platform.entity.CampusHealthSignalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampusHealthSignalRepository extends JpaRepository<CampusHealthSignalEntity, Long> {

    Optional<CampusHealthSignalEntity> findFirstByStudentIdOrderByObservedAtDescIdDesc(String studentId);

    List<CampusHealthSignalEntity> findByStudentIdOrderByObservedAtDescIdDesc(String studentId);
}