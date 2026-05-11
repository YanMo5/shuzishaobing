package com.campushealth.platform.repository;

import com.campushealth.platform.entity.StudentProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfileEntity, String> {
}