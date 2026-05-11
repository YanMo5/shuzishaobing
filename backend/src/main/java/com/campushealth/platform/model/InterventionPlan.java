package com.campushealth.platform.model;

import java.util.List;

public record InterventionPlan(
        List<String> immediateActions,
        List<String> followUpActions,
        List<String> supportingServices
) {
}
