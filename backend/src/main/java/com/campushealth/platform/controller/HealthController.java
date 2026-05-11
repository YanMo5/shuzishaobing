package com.campushealth.platform.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campushealth.platform.config.ApiAuthenticationFilter;
import com.campushealth.platform.dto.AdminLoginRequest;
import com.campushealth.platform.dto.CampusHealthSignalRequest;
import com.campushealth.platform.dto.ChangePasswordRequest;
import com.campushealth.platform.dto.LoginResponse;
import com.campushealth.platform.dto.ModelInferenceRequest;
import com.campushealth.platform.dto.ModelInferenceResponse;
import com.campushealth.platform.dto.StaffLoginRequest;
import com.campushealth.platform.dto.StaffRegistrationRequest;
import com.campushealth.platform.dto.StudentLoginRequest;
import com.campushealth.platform.dto.StudentPasswordResetRequest;
import com.campushealth.platform.dto.StudentRegistrationRequest;
import com.campushealth.platform.dto.StudentRegistrationResponse;
import com.campushealth.platform.dto.StudentRiskRequest;
import com.campushealth.platform.dto.StudentSummaryResponse;
import com.campushealth.platform.model.AuditEvent;
import com.campushealth.platform.model.CampusHealthSignal;
import com.campushealth.platform.model.CampusPrincipal;
import com.campushealth.platform.model.RiskAssessment;
import com.campushealth.platform.model.StaffProfile;
import com.campushealth.platform.model.StudentProfile;
import com.campushealth.platform.service.CampusPlatformService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 校园健康平台 REST API 控制器
 * 
 * <p>本控制器提供了校园健康平台的所有 REST API 端点，包括：
 * <ul>
 *   <li>学生相关接口：注册、登录、健康数据查询与提交</li>
 *   <li>教师相关接口：登录、注册（需管理员权限）、学生管理</li>
 *   <li>管理员相关接口：登录、密码修改、用户管理</li>
 *   <li>健康评估与模型推理接口</li>
 * </ul>
 * 
 * <p>所有需要认证的接口通过 {@link ApiAuthenticationFilter} 过滤器进行身份验证，
 * 认证成功后将用户信息存储在请求属性中。
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    /** 平台服务层，处理核心业务逻辑 */
    private final CampusPlatformService campusPlatformService;

    /**
     * 构造函数，注入平台服务依赖
     * @param campusPlatformService 平台服务实例
     */
    public HealthController(CampusPlatformService campusPlatformService) {
        this.campusPlatformService = campusPlatformService;
    }

    /**
     * 服务健康检查接口
     * <p>用于验证后端服务是否正常运行
     * @return 返回服务状态字符串
     */
    @GetMapping("/health/ping")
    public String ping() {
        return "campus-health-platform-ok";
    }

    /**
     * 学生注册接口
     * <p>允许学生自行注册账号，无需认证
     * @param request 学生注册请求，包含学号、密码、姓名、院系、专业、班级等信息
     * @return 注册成功后的响应，包含学生信息和访问令牌
     */
    @PostMapping("/students/register")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentRegistrationResponse registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
        return campusPlatformService.registerStudent(request);
    }

    /**
     * 学生登录接口
     * <p>学生使用学号和密码登录系统
     * @param request 登录请求，包含学号和密码
     * @return 登录成功后的响应，包含访问令牌和用户信息
     */
    @PostMapping("/students/login")
    public LoginResponse loginStudent(@Valid @RequestBody StudentLoginRequest request) {
        return campusPlatformService.loginStudent(request.studentId(), request.password());
    }

    /**
     * 获取学生健康摘要接口
     * <p>学生可查看自己的健康摘要信息
     * @param studentId 学生学号
     * @param request HTTP请求对象，用于获取认证信息
     * @return 学生健康摘要响应
     */
    @GetMapping("/students/{studentId}/summary")
    public StudentSummaryResponse summary(@PathVariable String studentId, HttpServletRequest request) {
        return campusPlatformService.getSummary(studentId, principal(request));
    }

    /**
     * 获取学生健康信号历史接口
     * <p>学生可查看自己的健康数据历史记录
     * @param studentId 学生学号
     * @param request HTTP请求对象，用于获取认证信息
     * @return 健康信号列表
     */
    @GetMapping("/students/{studentId}/signals")
    public List<CampusHealthSignal> signals(@PathVariable String studentId, HttpServletRequest request) {
        return campusPlatformService.getSignals(studentId, principal(request));
    }

    /**
     * 提交学生健康信号接口
     * <p>管理员或教师可为学生提交健康数据
     * @param studentId 学生学号
     * @param request 健康信号请求，包含睡眠、压力、营养等健康指标
     * @param httpServletRequest HTTP请求对象，用于获取认证信息
     * @return 提交成功后的健康信号记录
     */
    @PostMapping("/students/{studentId}/signals")
    @ResponseStatus(HttpStatus.CREATED)
    public CampusHealthSignal ingestSignal(@PathVariable String studentId,
                                           @Valid @RequestBody CampusHealthSignalRequest request,
                                           HttpServletRequest httpServletRequest) {
        return campusPlatformService.ingestSignal(studentId, request, principal(httpServletRequest));
    }

    /**
     * 健康风险评估接口
     * <p>对学生进行健康风险评估
     * @param request 风险评估请求，包含学生信息和健康观测数据
     * @param httpServletRequest HTTP请求对象，用于获取认证信息
     * @return 风险评估结果
     */
    @PostMapping("/assessments")
    public RiskAssessment assess(@Valid @RequestBody StudentRiskRequest request, HttpServletRequest httpServletRequest) {
        return campusPlatformService.assess(request, principal(httpServletRequest));
    }

    /**
     * 模型推理接口
     * <p>使用AI模型对学生健康数据进行分析推理
     * @param request 模型推理请求，包含学生ID和相关参数
     * @param httpServletRequest HTTP请求对象，用于获取认证信息
     * @return 模型推理响应
     */
    @PostMapping("/models/inference")
    public ModelInferenceResponse infer(@Valid @RequestBody ModelInferenceRequest request, HttpServletRequest httpServletRequest) {
        return campusPlatformService.infer(request, principal(httpServletRequest));
    }

    /**
     * 获取学生列表接口
     * <p>管理员或教师可查看所有学生列表
     * @param request HTTP请求对象，用于获取认证信息
     * @return 学生列表
     */
    @GetMapping("/students")
    public List<StudentProfile> listStudents(HttpServletRequest request) {
        return campusPlatformService.listStudents(principal(request));
    }

    /**
     * 教师注册接口
     * <p>管理员可注册新教师账号
     * @param request 教师注册请求，包含姓名、工号、密码、部门、职称等信息
     * @param httpServletRequest HTTP请求对象，用于获取认证信息
     * @return 注册成功后的教师信息
     */
    @PostMapping("/staff/register")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffProfile registerStaff(@Valid @RequestBody StaffRegistrationRequest request, HttpServletRequest httpServletRequest) {
        return campusPlatformService.registerStaff(request, principal(httpServletRequest));
    }

    /**
     * 教师登录接口
     * <p>教师使用工号和密码登录系统
     * @param request 登录请求，包含工号和密码
     * @return 登录成功后的响应，包含访问令牌和用户信息
     */
    @PostMapping("/staff/login")
    public LoginResponse loginStaff(@Valid @RequestBody StaffLoginRequest request) {
        return campusPlatformService.loginStaff(request.staffId(), request.password());
    }

    /**
     * 获取教师列表接口
     * <p>管理员可查看所有教师列表
     * @param request HTTP请求对象，用于获取认证信息
     * @return 教师列表
     */
    @GetMapping("/staff")
    public List<StaffProfile> listStaff(HttpServletRequest request) {
        return campusPlatformService.listStaff(principal(request));
    }

    /**
     * 管理员登录接口
     * <p>管理员使用用户名和密码登录系统
     * @param request 登录请求，包含用户名和密码
     * @return 登录成功后的响应，包含访问令牌和用户信息
     */
    @PostMapping("/admin/login")
    public LoginResponse loginAdmin(@Valid @RequestBody AdminLoginRequest request) {
        return campusPlatformService.loginAdmin(request.username(), request.password());
    }

    /**
     * 修改管理员密码接口
     * <p>管理员可修改自己的密码
     * @param request 密码修改请求，包含原密码和新密码
     * @param httpServletRequest HTTP请求对象，用于获取认证信息
     */
    @PostMapping("/admin/password")
    public void changeAdminPassword(@Valid @RequestBody ChangePasswordRequest request, HttpServletRequest httpServletRequest) {
        campusPlatformService.changeAdminPassword(request.oldPassword(), request.newPassword(), principal(httpServletRequest));
    }

    /**
     * 管理员添加学生接口
     * <p>管理员可添加新学生账号
     * @param request 学生注册请求
     * @param httpServletRequest HTTP请求对象，用于获取认证信息
     * @return 添加成功后的学生信息
     */
    @PostMapping("/admin/students")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentProfile createStudent(@Valid @RequestBody StudentRegistrationRequest request, HttpServletRequest httpServletRequest) {
        return campusPlatformService.createStudent(request, principal(httpServletRequest));
    }

    /**
     * 管理员删除学生接口
     * <p>管理员可删除学生账号
     * @param studentId 学生学号
     * @param httpServletRequest HTTP请求对象，用于获取认证信息
     */
    @DeleteMapping("/admin/students/{studentId}")
    public void deleteStudent(@PathVariable String studentId, HttpServletRequest httpServletRequest) {
        campusPlatformService.deleteStudent(studentId, principal(httpServletRequest));
    }

    /**
     * 管理员删除教师接口
     * <p>管理员可删除教师账号
     * @param staffId 教师工号
     * @param httpServletRequest HTTP请求对象，用于获取认证信息
     */
    @DeleteMapping("/admin/staff/{staffId}")
    public void deleteStaff(@PathVariable String staffId, HttpServletRequest httpServletRequest) {
        campusPlatformService.deleteStaff(staffId, principal(httpServletRequest));
    }

    /**
     * 教师添加学生接口
     * <p>教师可添加新学生账号
     * @param request 学生注册请求
     * @param httpServletRequest HTTP请求对象，用于获取认证信息
     * @return 添加成功后的学生信息
     */
    @PostMapping("/staff/students")
    @ResponseStatus(HttpStatus.CREATED)
    public StudentProfile createStudentByStaff(@Valid @RequestBody StudentRegistrationRequest request, HttpServletRequest httpServletRequest) {
        return campusPlatformService.createStudentByStaff(request, principal(httpServletRequest));
    }

    /**
     * 教师删除学生接口
     * <p>教师可删除学生账号
     * @param studentId 学生学号
     * @param httpServletRequest HTTP请求对象，用于获取认证信息
     */
    @DeleteMapping("/staff/students/{studentId}")
    public void deleteStudentByStaff(@PathVariable String studentId, HttpServletRequest httpServletRequest) {
        campusPlatformService.deleteStudentByStaff(studentId, principal(httpServletRequest));
    }

    /**
     * 教师重置学生密码接口
     * <p>教师可修改学生账号密码，便于统一发放或找回。
     * @param studentId 学生学号
     * @param request 密码重置请求，仅包含新密码
     * @param httpServletRequest HTTP请求对象，用于获取认证信息
     */
    @PostMapping("/staff/students/{studentId}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetStudentPasswordByStaff(@PathVariable String studentId,
                                            @Valid @RequestBody StudentPasswordResetRequest request,
                                            HttpServletRequest httpServletRequest) {
        campusPlatformService.resetStudentPasswordByStaff(studentId, request.newPassword(), principal(httpServletRequest));
    }

    /**
     * 获取审计日志接口
     * <p>管理员可查看系统操作审计日志
     * @param request HTTP请求对象，用于获取认证信息
     * @return 审计事件列表
     */
    @GetMapping("/audit/events")
    public List<AuditEvent> auditEvents(HttpServletRequest request) {
        return campusPlatformService.listAuditEvents(principal(request));
    }

    /**
     * 从请求中获取当前认证用户信息
     * <p>从HTTP请求属性中提取已认证的用户主体信息
     * @param request HTTP请求对象
     * @return 当前认证用户的主体信息
     * @throws org.springframework.web.server.ResponseStatusException 如果未认证则抛出401异常
     */
    private CampusPrincipal principal(HttpServletRequest request) {
        Object principal = request.getAttribute(ApiAuthenticationFilter.PRINCIPAL_ATTRIBUTE);
        if (principal instanceof CampusPrincipal campusPrincipal) {
            return campusPrincipal;
        }
        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
