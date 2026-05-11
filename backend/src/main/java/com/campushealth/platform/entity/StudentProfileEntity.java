package com.campushealth.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_profiles")
public class StudentProfileEntity {

    @Id
    @Column(length = 32, nullable = false)
    private String studentId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String college;

    @Column(nullable = false)
    private String major;

    @Column
    private String className;

    @Column(nullable = false)
    private int grade;

    @Column(nullable = false)
    private String dormitory;

    protected StudentProfileEntity() {
    }

    public StudentProfileEntity(String studentId, String name, String password, String college, String major, String className, int grade, String dormitory) {
        this.studentId = studentId;
        this.name = name;
        this.password = password;
        this.college = college;
        this.major = major;
        this.className = className;
        this.grade = grade;
        this.dormitory = dormitory;
    }

    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getCollege() { return college; }
    public String getMajor() { return major; }
    public String getClassName() { return className; }
    public int getGrade() { return grade; }
    public String getDormitory() { return dormitory; }
}