package com.gold.safefam.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SafeFam 공통 API 응답")
public record ApiResponse<T>(
        @Schema(example = "SUCCESS")
        String status,

        @Schema(example = "요청이 성공적으로 처리되었습니다.")
        String message,

        T data
) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>("SUCCESS", message, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>("ERROR", message, null);
    }
}
