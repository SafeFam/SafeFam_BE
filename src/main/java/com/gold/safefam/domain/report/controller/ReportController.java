package com.gold.safefam.domain.report.controller;

import com.gold.safefam.domain.report.dto.ReportRequest;
import com.gold.safefam.domain.report.dto.ReportResponse;
import com.gold.safefam.domain.report.service.ReportService;
import com.gold.safefam.domain.report.service.ReportService.SubmissionResult;
import com.gold.safefam.global.config.SwaggerConfig;
import com.gold.safefam.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 탐지 이력 신고 요청을 받는 HTTP 진입점이다.
 * 인증 사용자 소유권 검증과 익명 스냅샷 저장은 {@link ReportService}에 위임한다.
 */
@Tag(name = "07. 신고", description = "탐지 이력 익명 신고")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/analyses")
public class ReportController {

    private final ReportService reportService;

    /** 최초 신고는 201, 같은 분석의 재요청은 기존 신고를 200으로 반환한다. */
    @Operation(
            summary = "탐지 이력 신고",
            description = "문자 원문과 사용자 식별자를 제외한 분석 스냅샷만 저장합니다. 같은 분석의 재신고는 기존 결과를 반환합니다."
    )
    @PostMapping("/{analysisId}/report")
    public ResponseEntity<ApiResponse<ReportResponse>> submit(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long analysisId,
            @Valid @RequestBody ReportRequest request
    ) {
        SubmissionResult result = reportService.submit(userId, analysisId, request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        String message = result.created()
                ? "탐지 이력을 익명으로 신고했습니다."
                : "이미 신고된 탐지 이력입니다.";
        return ResponseEntity.status(status)
                .body(ApiResponse.success(message, result.response()));
    }
}
