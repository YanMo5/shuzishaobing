package com.campushealth.platform.service;

import com.campushealth.platform.entity.StudentProfileEntity;
import com.campushealth.platform.repository.StudentProfileRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CampusDataInitializer implements CommandLineRunner {

    private final StudentProfileRepository studentProfileRepository;

    public CampusDataInitializer(StudentProfileRepository studentProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
    }

    @Override
    public void run(String... args) {
        if (studentProfileRepository.count() > 0) {
            return;
        }
        studentProfileRepository.save(new StudentProfileEntity("S1001", "李明", "pass123", "计算机学院", "软件工程", "软工2班", 2, "A3-512"));
        studentProfileRepository.save(new StudentProfileEntity("S1002", "王婷", "pass123", "生命科学学院", "食品科学", "食品1班", 3, "B2-208"));
        studentProfileRepository.save(new StudentProfileEntity("S1003", "张强", "pass123", "管理学院", "工商管理", "工管1班", 1, "C1-319"));
    }
}