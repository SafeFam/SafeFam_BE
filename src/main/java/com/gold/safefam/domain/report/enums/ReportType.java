package com.gold.safefam.domain.report.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/** 사용자가 선택할 수 있는 탐지 이력 신고 분류다. */
@Schema(description = "사용자 신고 유형")
public enum ReportType {
    PHISHING,
    SPAM,
    OTHER
}
