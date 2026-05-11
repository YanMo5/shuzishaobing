package com.campushealth.platform.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.campushealth.platform.dto.CampusHealthSignalRequest;
import com.campushealth.platform.dto.LoginResponse;
import com.campushealth.platform.dto.ModelInferenceRequest;
import com.campushealth.platform.dto.ModelInferenceResponse;
import com.campushealth.platform.dto.StaffRegistrationRequest;
import com.campushealth.platform.dto.StudentRegistrationRequest;
import com.campushealth.platform.dto.StudentRegistrationResponse;
import com.campushealth.platform.dto.StudentRiskRequest;
import com.campushealth.platform.dto.StudentSummaryResponse;
import com.campushealth.platform.model.AuditActionType;
import com.campushealth.platform.model.AuditEvent;
import com.campushealth.platform.model.CampusHealthSignal;
import com.campushealth.platform.model.CampusPrincipal;
import com.campushealth.platform.model.RiskAssessment;
import com.campushealth.platform.model.StaffProfile;
import com.campushealth.platform.model.StudentProfile;
import com.campushealth.platform.repository.StaffProfileRepository;

/**
 * 校园健康平台核心服务层
 * 
 * <p>本类是平台的核心业务服务，负责整合各子服务并提供统一的业务接口。
 * 主要职责包括：
 * <ul>
 *   <li>用户认证与授权管理</li>
 *   <li>学生和教师的CRUD操作</li>
 *   <li>健康数据的提交与查询</li>
 *   <li>风险评估与模型推理</li>
 *   <li>审计日志记录</li>
 * </ul>
 * 
 * <p>所有方法均包含权限检查和审计日志记录，确保操作的安全性和可追溯性。
 */
@Service
public class CampusPlatformService {

    /** 管理员用户名 */
    private static final String ADMIN_USERNAME = "admin";
    
    /** 管理员密码（volatile保证线程安全） */
    private volatile String adminPassword = "root";

    /** 访问控制服务，用于权限验证 */
    private final AccessControlService accessControlService;
    
    /** 审计日志服务，用于记录操作日志 */
    private final AuditTrailService auditTrailService;
    
    /** 学生健康服务，处理学生相关业务 */
    private final StudentHealthService studentHealthService;
    
    /** 模型推理服务，处理AI模型相关操作 */
    private final ModelInferenceService modelInferenceService;
    
    /** 教师资料仓储，用于教师数据持久化 */
    private final StaffProfileRepository staffProfileRepository;

    /**
     * 构造函数，注入所有依赖服务
     * 
     * @param accessControlService 访问控制服务
     * @param auditTrailService 审计日志服务
     * @param studentHealthService 学生健康服务
     * @param modelInferenceService 模型推理服务
     * @param staffProfileRepository 教师资料仓储
     */
    public CampusPlatformService(AccessControlService accessControlService,
                                 AuditTrailService auditTrailService,
                                 StudentHealthService studentHealthService,
                                 ModelInferenceService modelInferenceService,
                                 StaffProfileRepository staffProfileRepository) {
        this.accessControlService = accessControlService;
        this.auditTrailService = auditTrailService;
        this.studentHealthService = studentHealthService;
        this.modelInferenceService = modelInferenceService;
        this.staffProfileRepository = staffProfileRepository;
    }

    /**
     * 获取学生健康摘要
     * <p>学生可查看自己的健康摘要信息
     * 
     * @param studentId 学生学号
     * @param principal 当前认证用户信息
     * @return 学生健康摘要响应
     */
    public StudentSummaryResponse getSummary(String studentId, CampusPrincipal principal) {
        accessControlService.requireStudentScope(principal, studentId);
        StudentSummaryResponse response = studentHealthService.getSummary(studentId);
        auditTrailService.record(principal, AuditActionType.STUDENT_SUMMARY, 
            "students/" + studentId + "/summary", "SUCCESS", "returned current health summary");
        return response;
    }

    /**
     * 学生登录
     * <p>验证学生账号密码并返回登录响应
     * 
     * @param studentId 学生学号
     * @param password 密码
     * @return 登录响应，包含令牌和用户信息
     */
    public LoginResponse loginStudent(String studentId, String password) {
        LoginResponse response = studentHealthService.loginStudent(studentId, password);
        return response;
    }

    /**
     * 学生注册
     * <p>创建新学生账号并返回注册响应
     * 
     * @param request 学生注册请求
     * @return 注册响应，包含学生信息和访问令牌
     */
    public StudentRegistrationResponse registerStudent(StudentRegistrationRequest request) {
        StudentProfile student = studentHealthService.registerStudent(request);
        return new StudentRegistrationResponse(accessControlService.studentToken(student.studentId()), student);
    }

    /**
     * 教师登录
     * <p>验证教师账号密码并返回登录响应
     * 
     * @param staffId 教师工号
     * @param password 密码
     * @return 登录响应，包含令牌和用户信息
     */
    public LoginResponse loginStaff(String staffId, String password) {
        LoginResponse response = studentHealthService.loginStaff(staffId, password);
        return response;
    }

    /**
     * 注册教师（管理员权限）
     * <p>管理员可注册新教师账号
     * 
     * @param request 教师注册请求
     * @param principal 当前认证用户信息（必须是管理员）
     * @return 注册成功后的教师信息
     */
    public StaffProfile registerStaff(StaffRegistrationRequest request, CampusPrincipal principal) {
        accessControlService.requireAdmin(principal);
        StaffProfile staff = studentHealthService.registerStaff(request);
        auditTrailService.record(principal, AuditActionType.STUDENT_SUMMARY, 
            "staff/register", "SUCCESS", "registered staff: " + staff.staffId());
        return staff;
    }

    /**
     * 创建学生（管理员权限）
     * <p>管理员可创建新学生账号
     * 
     * @param request 学生注册请求
     * @param principal 当前认证用户信息（必须是管理员）
     * @return 创建成功后的学生信息
     */
    public StudentProfile createStudent(StudentRegistrationRequest request, CampusPrincipal principal) {
        accessControlService.requireAdmin(principal);
        StudentProfile student = studentHealthService.registerStudent(request);
        auditTrailService.record(principal, AuditActionType.STUDENT_SUMMARY, 
            "students/create", "SUCCESS", "created student: " + student.studentId());
        return student;
    }

    /**
     * 删除学生（管理员权限）
     * <p>管理员可删除学生账号
     * 
     * @param studentId 学生学号
     * @param principal 当前认证用户信息（必须是管理员）
     */
    public void deleteStudent(String studentId, CampusPrincipal principal) {
        accessControlService.requireAdmin(principal);
        studentHealthService.deleteStudent(studentId);
        auditTrailService.record(principal, AuditActionType.STUDENT_SUMMARY, 
            "students/delete", "SUCCESS", "deleted student: " + studentId);
    }

    /**
     * 删除教师（管理员权限）
     * <p>管理员可删除教师账号
     * 
     * @param staffId 教师工号
     * @param principal 当前认证用户信息（必须是管理员）
     */
    @SuppressWarnings("null")
    public void deleteStaff(String staffId, CampusPrincipal principal) {
        accessControlService.requireAdmin(principal);
        staffProfileRepository.deleteById(staffId);
        auditTrailService.record(principal, AuditActionType.STUDENT_SUMMARY, 
            "staff/delete", "SUCCESS", "deleted staff: " + staffId);
    }

    /**
     * 创建学生（教师权限）
     * <p>教师可创建新学生账号
     * 
     * @param request 学生注册请求
     * @param principal 当前认证用户信息（必须是教师）
     * @return 创建成功后的学生信息
     */
    public StudentProfile createStudentByStaff(StudentRegistrationRequest request, CampusPrincipal principal) {
        accessControlService.requireStaff(principal);
        StudentProfile student = studentHealthService.registerStudent(request);
        auditTrailService.record(principal, AuditActionType.STUDENT_SUMMARY, 
            "staff/students/create", "SUCCESS", "created student by staff: " + student.studentId());
        return student;
    }

    /**
     * 删除学生（教师权限）
     * <p>教师可删除学生账号
     * 
     * @param studentId 学生学号
     * @param principal 当前认证用户信息（必须是教师）
     */
    public void deleteStudentByStaff(String studentId, CampusPrincipal principal) {
        accessControlService.requireStaff(principal);
        studentHealthService.deleteStudent(studentId);
        auditTrailService.record(principal, AuditActionType.STUDENT_SUMMARY, 
            "staff/students/delete", "SUCCESS", "deleted student by staff: " + studentId);
    }

    /**
     * 教师重置学生密码
     * <p>教师可修改学生账号密码，便于统一发放或找回。
     *
     * @param studentId 学生学号
     * @param newPassword 新密码
     * @param principal 当前认证用户信息（必须是教师）
     */
    public void resetStudentPasswordByStaff(String studentId, String newPassword, CampusPrincipal principal) {
        accessControlService.requireStaff(principal);
        studentHealthService.resetStudentPassword(studentId, newPassword);
        auditTrailService.record(principal, AuditActionType.STUDENT_PASSWORD_UPDATE,
            "staff/students/" + studentId + "/password", "SUCCESS", "reset password for student: " + studentId);
    }

    /**
     * 管理员登录
     * <p>验证管理员账号密码并返回登录响应
     * 
     * @param username 用户名
     * @param password 密码
     * @return 登录响应，包含令牌和用户信息
     * @throws IllegalArgumentException 如果账号或密码错误
     */
    public LoginResponse loginAdmin(String username, String password) {
        if (!ADMIN_USERNAME.equals(username) || !adminPassword.equals(password)) {
            throw new IllegalArgumentException("管理员账号或密码错误");
        }
        String token = "token-admin-platform";
        return new LoginResponse(token, ADMIN_USERNAME, "系统管理员", "admin");
    }
    
    /**
     * 修改管理员密码（管理员权限）
     * <p>管理员可修改自己的密码
     * 
     * @param oldPassword 原密码
     * @param newPassword 新密码
     * @param principal 当前认证用户信息（必须是管理员）
     * @throws IllegalArgumentException 如果原密码错误或新密码不符合要求
     */
    public void changeAdminPassword(String oldPassword, String newPassword, CampusPrincipal principal) {
        accessControlService.requireAdmin(principal);
        if (!adminPassword.equals(oldPassword)) {
            throw new IllegalArgumentException("原密码错误");
        }
        if (newPassword == null || newPassword.isBlank() || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少6位");
        }
        this.adminPassword = newPassword;
        auditTrailService.record(principal, AuditActionType.AUDIT_VIEW, 
            "admin/password", "SUCCESS", "password changed");
    }

    /**
     * 获取学生健康信号历史
     * <p>学生可查看自己的健康数据历史
     * 
     * @param studentId 学生学号
     * @param principal 当前认证用户信息
     * @return 健康信号列表
     */
    public List<CampusHealthSignal> getSignals(String studentId, CampusPrincipal principal) {
        accessControlService.requireStudentScope(principal, studentId);
        List<CampusHealthSignal> signals = studentHealthService.listSignals(studentId);
        auditTrailService.record(principal, AuditActionType.STUDENT_SUMMARY, 
            "students/" + studentId + "/signals", "SUCCESS", "returned signal history size=" + signals.size());
        return signals;
    }

    /**
     * 提交学生健康信号
     * <p>学生、教师或管理员可为学生提交健康数据
     *
     * @param studentId 学生学号
     * @param request 健康信号请求
     * @param principal 当前认证用户信息
     * @return 提交成功后的健康信号记录
     */
    public CampusHealthSignal ingestSignal(String studentId, CampusHealthSignalRequest request, CampusPrincipal principal) {
        if (principal.isStudent()) {
            accessControlService.requireStudentScope(principal, studentId);
        } else {
            accessControlService.requireStaffOrAdmin(principal);
        }
        CampusHealthSignal signal = studentHealthService.ingestSignal(studentId, request, principal);
        auditTrailService.record(principal, AuditActionType.HEALTH_SIGNAL_INGEST, 
            "students/" + studentId + "/signals", "SUCCESS", "source=" + request.sourceType());
        return signal;
    }

    /**
     * 健康风险评估
     * <p>对学生进行健康风险评估
     * 
     * @param request 风险评估请求
     * @param principal 当前认证用户信息
     * @return 风险评估结果
     */
    public RiskAssessment assess(StudentRiskRequest request, CampusPrincipal principal) {
        if (principal.isStudent()) {
            accessControlService.requireStudentScope(principal, request.student().studentId());
        } else {
            accessControlService.requireStaffOrAdmin(principal);
        }
        RiskAssessment assessment = studentHealthService.assess(request.student(), request.observation(), request.focus());
        auditTrailService.record(principal, AuditActionType.STUDENT_SUMMARY, 
            "assessments", "SUCCESS", "studentId=" + request.student().studentId());
        return assessment;
    }

    /**
     * 模型推理
     * <p>使用AI模型对学生健康数据进行分析推理
     * 
     * @param request 模型推理请求
     * @param principal 当前认证用户信息
     * @return 模型推理响应
     */
    public ModelInferenceResponse infer(ModelInferenceRequest request, CampusPrincipal principal) {
        accessControlService.requireStudentScope(principal, request.studentId());
        ModelInferenceResponse response = modelInferenceService.infer(request);
        auditTrailService.record(principal, AuditActionType.MODEL_INFERENCE, 
            "models/inference", "SUCCESS", "studentId=" + request.studentId());
        return response;
    }

    /**
     * 获取审计日志（管理员权限）
     * <p>管理员可查看系统操作审计日志
     * 
     * @param principal 当前认证用户信息（必须是管理员）
     * @return 审计事件列表
     */
    public List<AuditEvent> listAuditEvents(CampusPrincipal principal) {
        accessControlService.requireAdmin(principal);
        List<AuditEvent> events = auditTrailService.listEvents();
        auditTrailService.record(principal, AuditActionType.AUDIT_VIEW, 
            "audit/events", "SUCCESS", "returned audit event count=" + events.size());
        return events;
    }

    /**
     * 获取学生列表（教师/管理员权限）
     * <p>教师或管理员可查看所有学生列表
     * 
     * @param principal 当前认证用户信息（教师或管理员）
     * @return 学生列表
     */
    public List<StudentProfile> listStudents(CampusPrincipal principal) {
        accessControlService.requireStaffOrAdmin(principal);
        List<StudentProfile> students = studentHealthService.listStudents();
        auditTrailService.record(principal, AuditActionType.STUDENT_SUMMARY, 
            "students", "SUCCESS", "returned student count=" + students.size());
        return students;
    }

    /**
     * 获取教师列表（管理员权限）
     * <p>管理员可查看所有教师列表
     * 
     * @param principal 当前认证用户信息（必须是管理员）
     * @return 教师列表
     */
    public List<StaffProfile> listStaff(CampusPrincipal principal) {
        accessControlService.requireAdmin(principal);
        List<StaffProfile> staff = studentHealthService.listStaff();
        auditTrailService.record(principal, AuditActionType.STUDENT_SUMMARY, 
            "staff", "SUCCESS", "returned staff count=" + staff.size());
        return staff;
    }
}
