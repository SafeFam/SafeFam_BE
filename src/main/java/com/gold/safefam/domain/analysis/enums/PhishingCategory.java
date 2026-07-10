package com.gold.safefam.domain.analysis.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "피싱 유형")
public enum PhishingCategory {
    FINANCIAL_INSTITUTION,
    GOVERNMENT_AGENCY,
    LOAN,
    JOB,
    DELIVERY,
    MESSENGER,
    OTHER
}
