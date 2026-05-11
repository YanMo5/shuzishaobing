package com.campushealth.platform.repository;

import com.campushealth.platform.entity.StaffProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfileEntity, String> {
    Optional<StaffProfileEntity> findByStaffId(String staffId);
}