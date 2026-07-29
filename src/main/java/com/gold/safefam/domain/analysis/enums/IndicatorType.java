package com.gold.safefam.domain.analysis.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "위험 근거 유형")
public enum IndicatorType {
    IMPERSONATION,
    FINANCIAL_ACTION,
    SENSITIVE_INFORMATION,
    URGENCY,
    SHORTENED_URL,
    MALICIOUS_URL,

    AI_EVIDENCE,
    ANALYSIS_TRACK_FAILURE
}
