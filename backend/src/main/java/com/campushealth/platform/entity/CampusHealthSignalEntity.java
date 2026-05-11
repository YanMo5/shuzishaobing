package com.campushealth.platform.entity;

import com.campushealth.platform.model.HealthDataSourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "campus_health_signals")
public class CampusHealthSignalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private HealthDataSourceType sourceType;

    @Column(nullable = false)
    private double sleepHours;

    @Column(nullable = false)
    private int lateNightCountPerWeek;

    @Column(nullable = false)
    private int nutritionScore;

    @Column(nullable = false)
    private int stressScore;

    @Column(nullable = false)
    private int physicalActivityMinutesPerWeek;

    @Column(nullable = false)
    private int infectionContacts;

    @Column(nullable = false)
    private boolean feverReported;

    @Column(nullable = false)
    private boolean coughReported;

    @Column(nullable = false)
    private Instant observedAt;

    @Column(length = 500)
    private String note;

    protected CampusHealthSignalEntity() {
    }

    public CampusHealthSignalEntity(String studentId,
                                    HealthDataSourceType sourceType,
                                    double sleepHours,
                                    int lateNightCountPerWeek,
                                    int nutritionScore,
                                    int stressScore,
                                    int physicalActivityMinutesPerWeek,
                                    int infectionContacts,
                                    boolean feverReported,
                                    boolean coughReported,
                                    Instant observedAt,
                                    String note) {
        this.studentId = studentId;
        this.sourceType = sourceType;
        this.sleepHours = sleepHours;
        this.lateNightCountPerWeek = lateNightCountPerWeek;
        this.nutritionScore = nutritionScore;
        this.stressScore = stressScore;
        this.physicalActivityMinutesPerWeek = physicalActivityMinutesPerWeek;
        this.infectionContacts = infectionContacts;
        this.feverReported = feverReported;
        this.coughReported = coughReported;
        this.observedAt = observedAt;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public String getStudentId() {
        return studentId;
    }

    public HealthDataSourceType getSourceType() {
        return sourceType;
    }

    public double getSleepHours() {
        return sleepHours;
    }

    public int getLateNightCountPerWeek() {
        return lateNightCountPerWeek;
    }

    public int getNutritionScore() {
        return nutritionScore;
    }

    public int getStressScore() {
        return stressScore;
    }

    public int getPhysicalActivityMinutesPerWeek() {
        return physicalActivityMinutesPerWeek;
    }

    public int getInfectionContacts() {
        return infectionContacts;
    }

    public boolean isFeverReported() {
        return feverReported;
    }

    public boolean isCoughReported() {
        return coughReported;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public String getNote() {
        return note;
    }
}