package com.gold.safefam.domain.analysis.controller;

import com.gold.safefam.domain.analysis.dto.AnalysisFeedbackRequest;
import com.gold.safefam.domain.analysis.dto.AnalysisListItemResponse;
import com.gold.safefam.domain.analysis.dto.AnalysisRequest;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.service.AnalysisService;
import com.gold.safefam.global.config.SwaggerConfig;
import com.gold.safefam.global.response.ApiResponse;
import com.gold.safefam.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 문자 위험 분석과 인증 사용자의 탐지 이력 관리 HTTP API를 제공한다.
 * 인증 주체에서 사용자 ID를 받아 요청 검증 이후 실제 처리는 {@link AnalysisService}에 위임한다.
 */
@Tag(name = "3. 문자 분석", description = "금융 사기 문자 분석 및 탐지 이력 관리")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(
            summary = "문자 분석",
            description = "문자 패턴과 URL 형태를 규칙 기반으로 분석해 위험도와 탐지 근거를 반환합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<AnalysisResponse>> analyze(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AnalysisRequest request
    ) {
        AnalysisResponse response = analysisService.analyze(userId, request);
        return ResponseEntity.ok(ApiResponse.success("문자 분석이 완료되었습니다.", response));
    }

    /** 인증 사용자의 탐지 이력을 페이지 단위로 필터링해 반환한다. */
    @Operation(
            summary = "탐지 이력 목록 조회",
            description = "위험 단계, 피싱 유형, 날짜 범위로 필터링하며 최신순으로 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AnalysisListItemResponse>>> getAnalyses(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) PhishingCategory category,
            @Parameter(description = "조회 시작일")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @Parameter(description = "조회 종료일")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        PageResponse<AnalysisListItemResponse> response = analysisService.getAnalyses(
                userId,
                page,
                size,
                riskLevel,
                category,
                from,
                to
        );
        return ResponseEntity.ok(ApiResponse.success("탐지 이력 목록을 조회했습니다.", response));
    }

    /** 인증 사용자가 소유한 탐지 이력 한 건의 전체 분석 결과를 반환한다. */
    @Operation(summary = "탐지 이력 상세 조회")
    @GetMapping("/{analysisId}")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysis(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long analysisId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "탐지 이력을 조회했습니다.",
                analysisService.getAnalysis(userId, analysisId)
        ));
    }

    /** 인증 사용자가 소유한 탐지 이력과 연결 데이터를 삭제한다. */
    @Operation(summary = "탐지 이력 삭제")
    @DeleteMapping("/{analysisId}")
    public ResponseEntity<Void> deleteAnalysis(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long analysisId
    ) {
        analysisService.deleteAnalysis(userId, analysisId);
        return ResponseEntity.noContent().build();
    }

    /** 인증 사용자의 분석 피드백을 새로 저장하거나 기존 값으로부터 갱신한다. */
    @Operation(
            summary = "분석 결과 피드백",
            description = "정탐, 오탐, 미탐 여부를 저장해 탐지 품질 개선에 사용합니다."
    )
    @PostMapping("/{analysisId}/feedback")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long analysisId,
            @Valid @RequestBody AnalysisFeedbackRequest request
    ) {
        analysisService.submitFeedback(userId, analysisId, request);
        return ResponseEntity.ok(ApiResponse.success("분석 결과 피드백을 저장했습니다."));
    }
}
