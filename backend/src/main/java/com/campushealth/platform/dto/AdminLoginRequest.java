package com.campushealth.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank(message = "管理员账号不能为空")
        String username,
        @NotBlank(message = "密码不能为空")
        String password
) {
}