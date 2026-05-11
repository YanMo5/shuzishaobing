package com.campushealth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentPasswordResetRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, message = "新密码至少6位")
        String newPassword
) {
}