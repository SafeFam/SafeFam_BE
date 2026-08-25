package com.gold.safefam.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SafeFam 공통 API 응답")
public record ApiResponse<T>(
        @Schema(example = "SUCCESS")
        String status,

        @Schema(
                description = "실패 원인을 식별하는 안정적인 코드이며 성공 응답에서는 null입니다.",
                example = "US002",
                nullable = true
        )
        String code,

        @Schema(example = "요청이 성공적으로 처리되었습니다.")
        String message,

        T data
) {

    /** 기존 성공 응답 생성 코드와의 소스 호환성을 유지한다. */
    public ApiResponse(String status, String message, T data) {
        this(status, null, message, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", null, message, data);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>("SUCCESS", null, message, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>("ERROR", code, message, null);
    }
}
