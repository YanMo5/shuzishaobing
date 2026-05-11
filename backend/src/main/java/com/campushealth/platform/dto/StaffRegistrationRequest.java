package com.campushealth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffRegistrationRequest(
        @NotBlank(message = "姓名不能为空")
        String name,
        @NotBlank(message = "工号不能为空")
        String staffId,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, message = "密码至少6位")
        String password,
        String department,
        String title
) {
}