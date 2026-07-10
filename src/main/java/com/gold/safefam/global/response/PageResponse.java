package com.gold.safefam.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "페이지네이션 응답")
public record PageResponse<T>(
        List<T> content,

        @Schema(example = "0")
        int page,

        @Schema(example = "20")
        int size,

        @Schema(example = "42")
        long totalElements,

        @Schema(example = "3")
        int totalPages,

        @Schema(example = "false")
        boolean last
) {
}
