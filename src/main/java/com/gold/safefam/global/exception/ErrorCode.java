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
    FORBIDDEN(HttpStatus.FORBIDDEN, "AU006", "접근 권한이 없습니다."),
    PHONE_VERIFICATION_REQUIRED(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "AU007",
            "휴대폰 인증이 필요합니다."
    ),
    PHONE_VERIFICATION_NOT_FOUND(
            HttpStatus.BAD_REQUEST,
            "AU008",
            "휴대폰 인증 요청을 찾을 수 없습니다."
    ),
    INVALID_PHONE_VERIFICATION_CODE(
            HttpStatus.BAD_REQUEST,
            "AU009",
            "휴대폰 인증번호가 올바르지 않습니다."
    ),
    PHONE_VERIFICATION_EXPIRED(
            HttpStatus.BAD_REQUEST,
            "AU010",
            "휴대폰 인증번호가 만료되었습니다."
    ),
    PHONE_VERIFICATION_ATTEMPTS_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "AU011",
            "휴대폰 인증 시도 횟수를 초과했습니다."
    ),
    PHONE_VERIFICATION_RESEND_TOO_SOON(
            HttpStatus.TOO_MANY_REQUESTS,
            "AU012",
            "잠시 후 인증번호를 다시 요청해 주세요."
    ),
    SMS_SEND_FAILED(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AU013",
            "인증번호 발송에 실패했습니다."
    ),
    DUPLICATE_PHONE_NUMBER(
            HttpStatus.CONFLICT,
            "AU014",
            "이미 가입된 휴대폰 번호입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
