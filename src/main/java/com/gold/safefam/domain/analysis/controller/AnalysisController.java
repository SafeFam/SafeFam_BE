package com.gold.safefam.domain.analysis.controller;

import com.gold.safefam.domain.analysis.dto.AnalysisFeedbackRequest;
import com.gold.safefam.domain.analysis.dto.AnalysisListItemResponse;
import com.gold.safefam.domain.analysis.dto.AnalysisRequest;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

@Tag(name = "3. 문자 분석", description = "금융 사기 문자 분석 및 탐지 이력 관리")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Validated
@RestController
@RequestMapping("/api/v1/analyses")
public class AnalysisController {

    @Operation(
            summary = "문자 분석",
            description = "LLM 문맥 분석, URL 위협 검사, 규칙 기반 패턴 점수를 종합해 위험도를 반환합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<AnalysisResponse>> analyze(
            @Valid @RequestBody AnalysisRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(
            summary = "탐지 이력 목록 조회",
            description = "위험 단계, 피싱 유형, 날짜 범위로 필터링하며 최신순으로 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AnalysisListItemResponse>>> getAnalyses(
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
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(summary = "탐지 이력 상세 조회")
    @GetMapping("/{analysisId}")
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysis(
            @PathVariable Long analysisId
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(summary = "탐지 이력 삭제")
    @DeleteMapping("/{analysisId}")
    public ResponseEntity<ApiResponse<Void>> deleteAnalysis(
            @PathVariable Long analysisId
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Operation(
            summary = "분석 결과 피드백",
            description = "정탐, 오탐, 미탐 여부를 저장해 탐지 품질 개선에 사용합니다."
    )
    @PostMapping("/{analysisId}/feedback")
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @PathVariable Long analysisId,
            @Valid @RequestBody AnalysisFeedbackRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
