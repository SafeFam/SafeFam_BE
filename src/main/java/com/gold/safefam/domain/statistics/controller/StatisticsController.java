package com.gold.safefam.domain.statistics.controller;

import com.gold.safefam.domain.statistics.dto.StatisticsOverviewResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "4. 통계", description = "개인 탐지 이력 대시보드")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
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
}
