package com.gold.safefam.domain.analysis.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "분석 요청 유입 경로")
public enum AnalysisSource {
    AUTO,
    MANUAL
}
