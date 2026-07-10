package com.gold.safefam.domain.analysis.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "종합 위험 단계")
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
