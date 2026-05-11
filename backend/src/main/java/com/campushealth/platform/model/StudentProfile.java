package com.campushealth.platform.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record StudentProfile(
        @NotBlank String studentId,
        @NotBlank String name,
        @NotBlank String college,
        @NotBlank String major,
        @NotBlank String className,
        @Min(1) int grade,
        @NotBlank String dormitory
) {
}