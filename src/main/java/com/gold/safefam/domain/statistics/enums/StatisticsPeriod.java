package com.gold.safefam.domain.statistics.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/** 현재 시점을 기준으로 개인 탐지 통계를 조회할 기간을 나타낸다. */
@Schema(description = "통계 조회 기간")
public enum StatisticsPeriod {
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
    ALL
}
