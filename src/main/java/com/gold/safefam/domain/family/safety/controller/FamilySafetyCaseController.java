package com.gold.safefam.domain.family.safety.controller;

import com.gold.safefam.domain.family.safety.dto.FamilyCallResponse;
import com.gold.safefam.domain.family.safety.dto.FamilySafetyCaseResponse;
import com.gold.safefam.domain.family.safety.dto.FamilySafetyStatusRequest;
import com.gold.safefam.domain.family.safety.enums.FamilySafetyStatus;
import com.gold.safefam.domain.family.safety.service.FamilySafetyCaseService;
import com.gold.safefam.global.response.ApiResponse;
import com.gold.safefam.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 보호자가 가족 공동 대응 건을 조회하고 전화 및 최종 처리를 수행하는 REST API다.
 * 인증 사용자 ID를 서비스로 전달하며 실제 권한과 상태 전이 검증은 도메인 서비스가 담당한다.
 */
@Tag(name = "9. 가족 안전 대응", description = "HIGH 위험 탐지 이후 보호자 공동 대응 API")
@RestController
@RequestMapping("/api/v1/family/alerts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Validated
public class FamilySafetyCaseController {

    private final FamilySafetyCaseService safetyCaseService;

    /** 보호자에게 연결된 가족의 공동 대응 건 목록을 페이지로 반환한다. */
    @Operation(summary = "가족 안전 대응 건 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FamilySafetyCaseResponse>>> getCases(
            @AuthenticationPrincipal Long guardianId,
            @RequestParam(required = false) FamilySafetyStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "가족 안전 대응 건을 조회했습니다.",
                safetyCaseService.getCases(guardianId, status, page, size)
        ));
    }

    /** 공동 대응 상세와 마스킹된 위험 근거 및 현재 처리 상태를 반환한다. */
    @Operation(summary = "가족 안전 대응 건 상세 조회")
    @GetMapping("/{caseId}")
    public ResponseEntity<ApiResponse<FamilySafetyCaseResponse>> getCase(
            @AuthenticationPrincipal Long guardianId,
            @PathVariable Long caseId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "가족 안전 대응 건을 조회했습니다.",
                safetyCaseService.getCase(guardianId, caseId)
        ));
    }

    /** 통화 시도를 CONTACTING으로 기록한 뒤 앱에 가족 전화번호를 반환한다. */
    @Operation(summary = "가족에게 전화하기", description = "통화 시도를 기록하고 가족 전화번호를 반환합니다.")
    @PostMapping("/{caseId}/call")
    public ResponseEntity<ApiResponse<FamilyCallResponse>> startCall(
            @AuthenticationPrincipal Long guardianId,
            @PathVariable Long caseId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "가족 통화를 시작합니다.",
                safetyCaseService.startCall(guardianId, caseId)
        ));
    }

    /** 안전 확인 완료 또는 이미 송금함 결과를 최종 상태로 저장한다. */
    @Operation(summary = "가족 안전 확인 결과 처리", description = "SAFE_CONFIRMED 또는 TRANSFERRED로 처리합니다.")
    @PatchMapping("/{caseId}/status")
    public ResponseEntity<ApiResponse<FamilySafetyCaseResponse>> resolve(
            @AuthenticationPrincipal Long guardianId,
            @PathVariable Long caseId,
            @Valid @RequestBody FamilySafetyStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "가족 안전 확인 결과를 저장했습니다.",
                safetyCaseService.resolve(guardianId, caseId, request.status())
        ));
    }
}
