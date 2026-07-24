package com.gold.safefam.domain.statistics.controller;

import com.gold.safefam.domain.statistics.dto.StatisticsOverviewResponse;
import com.gold.safefam.domain.statistics.dto.TrendCardResponse;
import com.gold.safefam.domain.statistics.enums.StatisticsPeriod;
import com.gold.safefam.domain.statistics.service.StatisticsService;
import com.gold.safefam.global.config.SwaggerConfig;
import com.gold.safefam.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개인 탐지 대시보드와 전체 익명 트렌드 카드 조회 API를 제공한다.
 * 개인 통계는 인증 사용자 범위로, 트렌드는 월간 중·고위험 표본 전체로 집계한다.
 */
@Tag(name = "4. 통계", description = "개인 탐지 이력 대시보드")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /** 인증 사용자의 선택 기간별 탐지 통계를 반환한다. */
    @Operation(summary = "탐지 통계 조회", description = "총 탐지 수와 위험 단계·피싱 유형별 분포를 반환합니다.")
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<StatisticsOverviewResponse>> getOverview(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "LAST_30_DAYS") StatisticsPeriod period
    ) {
        StatisticsOverviewResponse response = statisticsService.getOverview(userId, period);
        return ResponseEntity.ok(ApiResponse.success("탐지 통계를 조회했습니다.", response));
    }

    /** 요청 월 또는 현재 월의 익명 탐지를 Top 3 유형과 Top 5 키워드 카드로 반환한다. */
    @Operation(
            summary = "월간 금융 사기 트렌드 카드 조회",
            description = "전체 사용자의 중·고위험 탐지를 익명 집계해 피싱 유형 Top 3와 위험 키워드 Top 5를 반환합니다."
    )
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<TrendCardResponse>> getTrendCards(
            @RequestParam(required = false)
            @jakarta.validation.constraints.Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])")
            String month
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "월간 금융 사기 트렌드를 조회했습니다.",
                statisticsService.getTrendCards(month)
        ));
    }
}
