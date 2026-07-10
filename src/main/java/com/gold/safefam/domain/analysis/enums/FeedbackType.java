package com.gold.safefam.domain.analysis.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "분석 결과 피드백 유형")
public enum FeedbackType {
    CORRECT,
    FALSE_POSITIVE,
    FALSE_NEGATIVE
}
