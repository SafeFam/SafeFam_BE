package com.gold.safefam.domain.chat.controller;

import com.gold.safefam.domain.chat.dto.ChatRequest;
import com.gold.safefam.domain.chat.dto.ChatResponse;
import com.gold.safefam.domain.chat.service.ChatService;
import com.gold.safefam.global.config.SwaggerConfig;
import com.gold.safefam.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "10. 챗봇", description = "분석 결과 기반 금융 사기 대응 상담")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "분석 결과 기반 멀티턴 상담",
            description = "본인 소유 분석 결과의 위험도와 판단 근거를 자동으로 AI에 전달합니다. "
                    + "클라이언트는 매 요청에 현재 대화 이력을 함께 전송해야 합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ChatResponse>> chat(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChatRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "챗봇 응답을 생성했습니다.",
                chatService.chat(userId, request)
        ));
    }
}
