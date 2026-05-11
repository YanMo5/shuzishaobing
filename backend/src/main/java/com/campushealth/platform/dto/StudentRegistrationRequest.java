package com.campushealth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentRegistrationRequest(
        @NotBlank(message = "姓名不能为空")
        String name,
        @NotBlank(message = "学号不能为空")
        String studentId,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, message = "密码至少6位")
        String password,
        String college,
        String major,
        String className,
        Integer grade,
        String dormitory
) {
}