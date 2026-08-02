package com.gold.safefam.domain.whitelist.controller;

import com.gold.safefam.domain.whitelist.dto.WhitelistCheckResponse;
import com.gold.safefam.domain.whitelist.dto.WhitelistRequest;
import com.gold.safefam.domain.whitelist.dto.WhitelistResponse;
import com.gold.safefam.domain.whitelist.service.WhitelistService;
import com.gold.safefam.global.config.SwaggerConfig;
import com.gold.safefam.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

import java.util.List;

/**
 * 인증 사용자의 신뢰 발신자를 관리하는 HTTP 진입점이다.
 * 등록·목록·삭제와 문자 분석 호출 전 프리패스 여부 확인 API를 제공한다.
 */
@Tag(name = "06. 화이트리스트", description = "신뢰 발신자 관리 및 분석 프리패스 확인")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/whitelists")
public class WhitelistController {

    private final WhitelistService whitelistService;

    /** 발신자와 선택 라벨을 정규화해 현재 사용자의 화이트리스트에 등록한다. */
    @Operation(summary = "화이트리스트 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<WhitelistResponse>> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WhitelistRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "화이트리스트에 발신자를 등록했습니다.",
                        whitelistService.create(userId, request)
                ));
    }

    /** 현재 사용자가 등록한 화이트리스트를 최신 등록순으로 반환한다. */
    @Operation(summary = "화이트리스트 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<WhitelistResponse>>> getAll(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "화이트리스트를 조회했습니다.",
                whitelistService.getAll(userId)
        ));
    }

    /** 입력 발신자가 현재 사용자의 화이트리스트에 있는지 분석 호출 전에 확인한다. */
    @Operation(
            summary = "화이트리스트 여부 확인",
            description = "whitelisted가 true이면 클라이언트는 문자 분석 API 호출을 생략할 수 있습니다."
    )
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<WhitelistCheckResponse>> check(
            @AuthenticationPrincipal Long userId,
            @RequestParam @NotBlank @Size(max = 100) String sender
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "화이트리스트 여부를 확인했습니다.",
                whitelistService.check(userId, sender)
        ));
    }

    /** 현재 사용자가 소유한 화이트리스트 항목 한 건을 삭제한다. */
    @Operation(summary = "화이트리스트 삭제")
    @DeleteMapping("/{whitelistId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long whitelistId
    ) {
        whitelistService.delete(userId, whitelistId);
        return ResponseEntity.noContent().build();
    }
}
