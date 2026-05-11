package com.campushealth.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModelInferenceRequest(
        @NotBlank String studentId,
        @Size(max = 500) String prompt,
        @Size(max = 120) String focus
) {
}