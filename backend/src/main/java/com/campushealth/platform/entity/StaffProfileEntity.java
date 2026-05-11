package com.campushealth.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff_profiles")
public class StaffProfileEntity {

    @Id
    @Column(length = 32, nullable = false)
    private String staffId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column
    private String department;

    @Column
    private String title;

    protected StaffProfileEntity() {
    }

    public StaffProfileEntity(String staffId, String name, String password, String department, String title) {
        this.staffId = staffId;
        this.name = name;
        this.password = password;
        this.department = department;
        this.title = title;
    }

    public String getStaffId() { return staffId; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getDepartment() { return department; }
    public String getTitle() { return title; }
}