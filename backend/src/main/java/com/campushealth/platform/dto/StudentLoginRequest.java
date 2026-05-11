package com.campushealth.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record StudentLoginRequest(
        @NotBlank(message = "学号不能为空")
        String studentId,
        @NotBlank(message = "密码不能为空")
        String password
) {
}