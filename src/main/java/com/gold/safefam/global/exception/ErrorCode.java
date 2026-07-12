package com.gold.safefam.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 오류가 발생했습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AU001", "인증이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AU002", "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AU003", "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(
            HttpStatus.UNAUTHORIZED,
            "AU004",
            "저장된 Refresh Token을 찾을 수 없습니다."
    ),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AU005", "이미 가입된 이메일입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AU006", "접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
