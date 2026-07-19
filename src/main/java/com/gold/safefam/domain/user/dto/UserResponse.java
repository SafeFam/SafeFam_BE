package com.gold.safefam.domain.user.dto;

import com.gold.safefam.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자 정보")
public record UserResponse(
        @Schema(example = "1")
        Long userId,

        @Schema(example = "010-****-0000")
        String phoneNumber,

        @Schema(example = "김안전")
        String name,

        @Schema(example = "USER")
        String role,

        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                maskPhoneNumber(user.getPhoneNumber()),
                user.getName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }

    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 10) return phoneNumber;
        return phoneNumber.replaceAll("(\\d{3})(\\d{3,4})(\\d{4})", "$1-****-$3");
    }
}