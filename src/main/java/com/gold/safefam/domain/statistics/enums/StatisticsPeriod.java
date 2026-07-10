package com.gold.safefam.domain.statistics.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "통계 조회 기간")
public enum StatisticsPeriod {
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
    ALL
}
