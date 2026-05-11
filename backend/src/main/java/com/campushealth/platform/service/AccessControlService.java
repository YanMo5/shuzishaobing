package com.campushealth.platform.service;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.campushealth.platform.model.CampusPrincipal;
import com.campushealth.platform.model.UserRole;
import com.campushealth.platform.repository.StaffProfileRepository;
import com.campushealth.platform.repository.StudentProfileRepository;

/**
 * 访问控制服务
 * 
 * <p>本服务负责处理用户认证和权限验证，主要职责包括：
 * <ul>
 *   <li>从API令牌解析用户主体信息</li>
 *   <li>生成用户访问令牌</li>
 *   <li>验证用户角色权限</li>
 *   <li>检查学生数据访问范围</li>
 * </ul>
 * 
 * <p>支持三种用户角色：学生(STUDENT)、教师(STAFF)、管理员(ADMIN)
 */
@Service
public class AccessControlService {

    /** 学生令牌前缀 */
    private static final String STUDENT_TOKEN_PREFIX = "token-student-";
    
    /** 教师令牌前缀 */
    private static final String STAFF_TOKEN_PREFIX = "staff-";

    /**
     * 静态令牌目录
     * <p>存储预设的管理员和教师令牌，用于系统初始化测试
     */
    private final Map<String, CampusPrincipal> tokenDirectory = Map.of(
            "token-staff-health", new CampusPrincipal("u-staff-1", "辅导员张老师", UserRole.STAFF, null),
            "token-admin-platform", new CampusPrincipal("u-admin-1", "平台管理员", UserRole.ADMIN, null)
    );

    /** 学生资料仓储，用于验证学生账号 */
    private final StudentProfileRepository studentProfileRepository;
    
    /** 教师资料仓储，用于验证教师账号 */
    private final StaffProfileRepository staffProfileRepository;

    /**
     * 构造函数，注入仓储依赖
     * 
     * @param studentProfileRepository 学生资料仓储
     * @param staffProfileRepository 教师资料仓储
     */
    public AccessControlService(StudentProfileRepository studentProfileRepository,
                               StaffProfileRepository staffProfileRepository) {
        this.studentProfileRepository = studentProfileRepository;
        this.staffProfileRepository = staffProfileRepository;
    }

    /**
     * 从API令牌解析用户主体信息
     * <p>支持三种令牌格式：
     * <ul>
     *   <li>静态令牌：直接从tokenDirectory查找</li>
     *   <li>学生令牌：格式为 token-student-{studentId}</li>
     *   <li>教师令牌：格式为 staff-{staffId}</li>
     * </ul>
     * 
     * @param apiToken API访问令牌
     * @return 用户主体信息，如果令牌无效则返回空
     */
    public Optional<CampusPrincipal> resolvePrincipal(String apiToken) {
        if (apiToken == null || apiToken.isBlank()) {
            return Optional.empty();
        }

        // 首先检查静态令牌
        CampusPrincipal staticPrincipal = tokenDirectory.get(apiToken);
        if (staticPrincipal != null) {
            return Optional.of(staticPrincipal);
        }

        String lowerToken = apiToken.toLowerCase(Locale.ROOT);

        // 处理学生令牌
        if (lowerToken.startsWith(STUDENT_TOKEN_PREFIX)) {
            String studentId = apiToken.substring(STUDENT_TOKEN_PREFIX.length()).toUpperCase(Locale.ROOT);
            return studentProfileRepository.findById(Objects.requireNonNull(studentId, "studentId"))
                    .map(entity -> new CampusPrincipal(
                            "u-" + studentId.toLowerCase(Locale.ROOT),
                            entity.getName(),
                            UserRole.STUDENT,
                            studentId
                    ));
        }

        // 处理教师令牌
        if (lowerToken.startsWith(STAFF_TOKEN_PREFIX)) {
            String staffId = apiToken.substring(STAFF_TOKEN_PREFIX.length()).toUpperCase(Locale.ROOT);
            return staffProfileRepository.findById(Objects.requireNonNull(staffId, "staffId"))
                    .map(entity -> new CampusPrincipal(
                            "u-" + staffId.toLowerCase(Locale.ROOT),
                            entity.getName(),
                            UserRole.STAFF,
                            null
                    ));
        }

        return Optional.empty();
    }

    /**
     * 生成学生访问令牌
     * 
     * @param studentId 学生学号
     * @return 学生访问令牌
     */
    public String studentToken(String studentId) {
        return STUDENT_TOKEN_PREFIX + studentId.toLowerCase(Locale.ROOT);
    }

    /**
     * 生成教师访问令牌
     * 
     * @param staffId 教师工号
     * @return 教师访问令牌
     */
    public String staffToken(String staffId) {
        return STAFF_TOKEN_PREFIX + staffId.toLowerCase(Locale.ROOT);
    }

    /**
     * 要求用户必须已认证
     * 
     * @param principal 用户主体信息
     * @return 用户主体信息（如果已认证）
     * @throws ResponseStatusException 如果未认证则抛出401异常
     */
    public CampusPrincipal requireAuthenticated(CampusPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication principal");
        }
        return principal;
    }

    /**
     * 要求用户必须是教师或管理员
     * 
     * @param principal 用户主体信息
     * @throws ResponseStatusException 如果权限不足则抛出403异常
     */
    public void requireStaffOrAdmin(CampusPrincipal principal) {
        requireAuthenticated(principal);
        if (!(principal.isStaff() || principal.isAdmin())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff or admin role required");
        }
    }

    /**
     * 要求用户必须是管理员
     * 
     * @param principal 用户主体信息
     * @throws ResponseStatusException 如果权限不足则抛出403异常
     */
    public void requireAdmin(CampusPrincipal principal) {
        requireAuthenticated(principal);
        if (!principal.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }

    /**
     * 要求用户必须是教师
     * 
     * @param principal 用户主体信息
     * @throws ResponseStatusException 如果权限不足则抛出403异常
     */
    public void requireStaff(CampusPrincipal principal) {
        requireAuthenticated(principal);
        if (!principal.isStaff()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff role required");
        }
    }

    /**
     * 检查学生数据访问范围
     * <p>学生只能访问自己的数据，管理员和教师可以访问所有学生数据
     * 
     * @param principal 用户主体信息
     * @param studentId 要访问的学生学号
     * @throws ResponseStatusException 如果无权访问则抛出403异常
     */
    public void requireStudentScope(CampusPrincipal principal, String studentId) {
        requireAuthenticated(principal);
        // 管理员和教师可以访问所有学生数据
        if (principal.isAdmin() || principal.isStaff()) {
            return;
        }
        // 学生只能访问自己的数据
        if (principal.isStudent() && studentId.equals(principal.studentId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student may only access own records");
    }
}
