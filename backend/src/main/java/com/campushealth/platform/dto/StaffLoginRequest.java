package com.campushealth.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record StaffLoginRequest(
        @NotBlank(message = "工号不能为空")
        String staffId,
        @NotBlank(message = "密码不能为空")
        String password
) {
}