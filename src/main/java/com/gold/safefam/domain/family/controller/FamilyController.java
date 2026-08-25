package com.gold.safefam.domain.family.controller;

import com.gold.safefam.domain.analysis.dto.AnalysisListItemResponse;
import com.gold.safefam.domain.family.dto.*;
import com.gold.safefam.domain.family.service.FamilyService;
import com.gold.safefam.global.response.ApiResponse;
import com.gold.safefam.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "08. 가족", description = "가족 보호 모드 API")
@RestController
@RequestMapping("/api/v1/family")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FamilyController {

    private final FamilyService familyService;

    @Operation(summary = "초대 코드 / QR 토큰 생성", description = "보호자가 초대 코드와 QR 토큰을 생성합니다. 유효시간 10분.")
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<FamilyInviteResponse>> createInvite(
            @AuthenticationPrincipal Long protectorId
    ) {
        FamilyInviteResponse response = familyService.createInvite(protectorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("SUCCESS", "초대 코드가 생성되었습니다.", response));
    }

    @Operation(summary = "초대 코드로 연결 수락", description = "피보호자가 초대 코드를 입력하여 보호자와 연결됩니다.")
    @PostMapping("/link/code")
    public ResponseEntity<ApiResponse<Void>> linkByCode(
            @AuthenticationPrincipal Long wardId,
            @Valid @RequestBody FamilyLinkByCodeRequest request
    ) {
        familyService.linkByCode(wardId, request.inviteCode());
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "가족으로 연결되었습니다.", null));
    }

    @Operation(summary = "QR 토큰으로 연결 수락", description = "피보호자가 QR 스캔으로 보호자와 연결됩니다.")
    @PostMapping("/link/qr")
    public ResponseEntity<ApiResponse<Void>> linkByQr(
            @AuthenticationPrincipal Long wardId,
            @Valid @RequestBody FamilyLinkByQrRequest request
    ) {
        familyService.linkByQr(wardId, request.qrToken());
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "가족으로 연결되었습니다.", null));
    }

    @Operation(summary = "연결 해제", description = "보호자 또는 피보호자가 연결을 해제합니다.")
    @DeleteMapping("/{linkId}")
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal Long requesterId,
            @PathVariable Long linkId
    ) {
        familyService.revoke(requesterId, linkId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "가족 목록 조회",
            description = "로그인 사용자가 보호자 또는 피보호자로 참여한 활성 가족 연결을 조회합니다."
    )
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<FamilyMemberResponse>>> getMembers(
            @AuthenticationPrincipal Long userId
    ) {
        List<FamilyMemberResponse> members = familyService.getMembers(userId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "가족 목록을 조회했습니다.", members));
    }

    @Operation(summary = "피보호자 탐지 이력 조회", description = "보호자가 피보호자의 탐지 이력을 조회합니다.")
    @GetMapping("/ward/{wardId}/logs")
    public ResponseEntity<ApiResponse<PageResponse<AnalysisListItemResponse>>> getWardLogs(
            @AuthenticationPrincipal Long protectorId,
            @PathVariable Long wardId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageResponse<AnalysisListItemResponse> logs = familyService.getWardLogs(protectorId, wardId, page, size);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "피보호자 탐지 이력을 조회했습니다.", logs));
    }

    @Operation(summary = "관계 설정", description = "보호자가 피보호자 관계를 설정합니다.")
    @PatchMapping("/{linkId}")
    public ResponseEntity<ApiResponse<Void>> updateRelationship(
            @AuthenticationPrincipal Long protectorId,
            @PathVariable Long linkId,
            @Valid @RequestBody FamilyUpdateRelationshipRequest request
    ) {
        familyService.updateRelationship(protectorId, linkId, request.relationship());
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS", "관계가 설정되었습니다.", null));
    }
}
