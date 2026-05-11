package com.campushealth.platform.service;

import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.campushealth.platform.dto.CampusHealthSignalRequest;
import com.campushealth.platform.dto.LoginResponse;
import com.campushealth.platform.dto.StaffRegistrationRequest;
import com.campushealth.platform.dto.StudentRegistrationRequest;
import com.campushealth.platform.dto.StudentSummaryResponse;
import com.campushealth.platform.entity.CampusHealthSignalEntity;
import com.campushealth.platform.entity.StaffProfileEntity;
import com.campushealth.platform.entity.StudentProfileEntity;
import com.campushealth.platform.model.CampusHealthSignal;
import com.campushealth.platform.model.HealthObservation;
import com.campushealth.platform.model.RiskAssessment;
import com.campushealth.platform.model.StaffProfile;
import com.campushealth.platform.model.StudentProfile;
import com.campushealth.platform.repository.CampusHealthSignalRepository;
import com.campushealth.platform.repository.StaffProfileRepository;
import com.campushealth.platform.repository.StudentProfileRepository;

@Service
public class StudentHealthService {

    private final RiskAssessmentService riskAssessmentService;
    private final CampusDataIngestionService campusDataIngestionService;
    private final StudentProfileRepository studentProfileRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final CampusHealthSignalRepository campusHealthSignalRepository;
    private final AccessControlService accessControlService;

    public StudentHealthService(RiskAssessmentService riskAssessmentService,
                                CampusDataIngestionService campusDataIngestionService,
                                StudentProfileRepository studentProfileRepository,
                                StaffProfileRepository staffProfileRepository,
                                CampusHealthSignalRepository campusHealthSignalRepository,
                                AccessControlService accessControlService) {
        this.riskAssessmentService = riskAssessmentService;
        this.campusDataIngestionService = campusDataIngestionService;
        this.studentProfileRepository = studentProfileRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.campusHealthSignalRepository = campusHealthSignalRepository;
        this.accessControlService = accessControlService;
    }

    public StudentSummaryResponse getSummary(String studentId) {
        StudentProfile student = findStudent(studentId);
        HealthObservation observation = resolveObservation(studentId);
        RiskAssessment assessment = riskAssessmentService.assess(student, observation);
        return new StudentSummaryResponse(student, observation, assessment);
    }

    public RiskAssessment assess(StudentProfile student, HealthObservation observation) {
        return riskAssessmentService.assess(student, observation);
    }

    public RiskAssessment assess(StudentProfile student, HealthObservation observation, String focus) {
        return riskAssessmentService.assess(student, observation, focus);
    }

    @SuppressWarnings("null")
    public StudentProfile findStudent(String studentId) {
        String normalizedStudentId = Objects.requireNonNull(studentId, "studentId").trim().toUpperCase(Locale.ROOT);
        StudentProfileEntity entity = studentProfileRepository.findById(normalizedStudentId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown student id: " + studentId));
        return new StudentProfile(entity.getStudentId(), entity.getName(), entity.getCollege(), entity.getMajor(), entity.getClassName(), entity.getGrade(), entity.getDormitory());
    }

    @SuppressWarnings("null")
    public StudentProfile registerStudent(StudentRegistrationRequest request) {
        String normalizedStudentId = Objects.requireNonNull(request.studentId(), "studentId").trim().toUpperCase(Locale.ROOT);
        String normalizedName = request.name().trim();
        String normalizedPassword = request.password();

        StudentProfileEntity existing = studentProfileRepository.findById(normalizedStudentId).orElse(null);
        String college = resolveString(request.college(), existing == null ? "待完善学院" : existing.getCollege());
        String major = resolveString(request.major(), existing == null ? "待完善专业" : existing.getMajor());
        String className = resolveString(request.className(), existing == null ? "待完善班级" : existing.getClassName());
        String dormitory = resolveString(request.dormitory(), existing == null ? "待完善宿舍" : existing.getDormitory());
        Integer requestedGrade = request.grade();
        int grade = requestedGrade != null ? requestedGrade : (existing == null ? 1 : existing.getGrade());

        String password = normalizedPassword;
        if (existing != null && normalizedPassword == null) {
            password = existing.getPassword();
        }

        StudentProfileEntity saved = studentProfileRepository.save(new StudentProfileEntity(
            normalizedStudentId,
            normalizedName,
            password,
            college,
            major,
            className,
            grade,
            dormitory
        ));
        return new StudentProfile(saved.getStudentId(), saved.getName(), saved.getCollege(), saved.getMajor(), saved.getClassName(), saved.getGrade(), saved.getDormitory());
    }

    @SuppressWarnings("null")
    public void deleteStudent(String studentId) {
        String normalizedStudentId = studentId.trim().toUpperCase(Locale.ROOT);
        if (!studentProfileRepository.existsById(normalizedStudentId)) {
            throw new IllegalArgumentException("学生不存在");
        }
        studentProfileRepository.deleteById(normalizedStudentId);
    }

    @SuppressWarnings("null")
    public void resetStudentPassword(String studentId, String newPassword) {
        String normalizedStudentId = Objects.requireNonNull(studentId, "studentId").trim().toUpperCase(Locale.ROOT);
        String password = Objects.requireNonNull(newPassword, "newPassword");
        StudentProfileEntity existing = studentProfileRepository.findById(normalizedStudentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));

        studentProfileRepository.save(new StudentProfileEntity(
                existing.getStudentId(),
                existing.getName(),
                password,
                existing.getCollege(),
                existing.getMajor(),
                existing.getClassName(),
                existing.getGrade(),
                existing.getDormitory()
        ));
    }

    @SuppressWarnings("null")
    public LoginResponse loginStudent(String studentId, String password) {
        String normalizedStudentId = studentId.trim().toUpperCase(Locale.ROOT);
        StudentProfileEntity entity = studentProfileRepository.findById(normalizedStudentId)
                .orElseThrow(() -> new IllegalArgumentException("学号或密码错误"));

        if (!entity.getPassword().equals(password)) {
            throw new IllegalArgumentException("学号或密码错误");
        }

        String token = "token-student-" + entity.getStudentId().toLowerCase(Locale.ROOT);
        return new LoginResponse(token, entity.getStudentId(), entity.getName(), "student");
    }

    @SuppressWarnings("null")
    public StaffProfile registerStaff(StaffRegistrationRequest request) {
        String normalizedStaffId = Objects.requireNonNull(request.staffId(), "staffId").trim().toUpperCase(Locale.ROOT);
        String normalizedName = request.name().trim();

        StaffProfileEntity existing = staffProfileRepository.findById(normalizedStaffId).orElse(null);
        String department = resolveString(request.department(), existing == null ? "待完善部门" : existing.getDepartment());
        String title = resolveString(request.title(), existing == null ? "待完善职称" : existing.getTitle());

        String password = request.password();
        if (existing != null && password == null) {
            password = existing.getPassword();
        }

        StaffProfileEntity saved = staffProfileRepository.save(new StaffProfileEntity(
            normalizedStaffId,
            normalizedName,
            password,
            department,
            title
        ));
        return new StaffProfile(saved.getStaffId(), saved.getName(), saved.getDepartment(), saved.getTitle());
    }

    @SuppressWarnings("null")
    public LoginResponse loginStaff(String staffId, String password) {
        String normalizedStaffId = staffId.trim().toUpperCase(Locale.ROOT);
        StaffProfileEntity entity = staffProfileRepository.findById(normalizedStaffId)
                .orElseThrow(() -> new IllegalArgumentException("工号或密码错误"));

        if (!entity.getPassword().equals(password)) {
            throw new IllegalArgumentException("工号或密码错误");
        }

        String token = accessControlService.staffToken(entity.getStaffId());
        return new LoginResponse(token, entity.getStaffId(), entity.getName(), "staff");
    }

    public HealthObservation resolveObservation(String studentId) {
        return campusDataIngestionService.latestSignal(studentId)
                .map(this::toObservation)
                .orElseGet(() -> buildSampleObservation(studentId));
    }

    public CampusHealthSignal ingestSignal(String studentId, CampusHealthSignalRequest request, com.campushealth.platform.model.CampusPrincipal principal) {
        return campusDataIngestionService.ingest(studentId, request, principal);
    }

    public java.util.List<CampusHealthSignal> listSignals(String studentId) {
        return campusHealthSignalRepository.findByStudentIdOrderByObservedAtDescIdDesc(studentId)
                .stream()
                .map(this::toSignal)
                .toList();
    }

    public java.util.List<StudentProfile> listStudents() {
        return studentProfileRepository.findAll()
                .stream()
                .map(entity -> new StudentProfile(entity.getStudentId(), entity.getName(), entity.getCollege(), entity.getMajor(), entity.getClassName(), entity.getGrade(), entity.getDormitory()))
                .toList();
    }

    public java.util.List<StaffProfile> listStaff() {
        return staffProfileRepository.findAll()
                .stream()
                .map(entity -> new StaffProfile(entity.getStaffId(), entity.getName(), entity.getDepartment(), entity.getTitle()))
                .toList();
    }

    private HealthObservation toObservation(CampusHealthSignal signal) {
        return new HealthObservation(
                signal.sleepHours(),
                signal.lateNightCountPerWeek(),
                signal.nutritionScore(),
                signal.stressScore(),
                signal.physicalActivityMinutesPerWeek(),
                signal.infectionContacts(),
                signal.feverReported(),
                signal.coughReported()
        );
    }

    private HealthObservation buildSampleObservation(String studentId) {
        return switch (studentId) {
            case "S1001" -> new HealthObservation(5.5, 4, 52, 78, 60, 1, false, true);
            case "S1002" -> new HealthObservation(6.8, 2, 76, 62, 140, 0, false, false);
            case "S1003" -> new HealthObservation(7.4, 1, 84, 40, 180, 0, false, false);
            default -> new HealthObservation(7.0, 0, 80, 40, 120, 0, false, false);
        };
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

    private String resolveString(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}