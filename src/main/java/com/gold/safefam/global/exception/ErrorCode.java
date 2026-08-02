package com.gold.safefam.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 도메인 예외를 HTTP 상태·내부 코드·사용자 메시지로 일관되게 매핑한다.
 * 화이트리스트 오류는 WL 접두사를 사용해 다른 도메인과 구분한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력입니다."),
    RATE_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "C002",
            "요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."
    ),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 오류가 발생했습니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AU001", "인증이 필요합니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AU002", "전화번호 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AU003", "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(
            HttpStatus.UNAUTHORIZED,
            "AU004",
            "저장된 Refresh Token을 찾을 수 없습니다."
    ),
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
    ),

    /** 존재하지 않거나 인증 사용자가 소유하지 않은 분석 이력을 동일하게 숨긴다. */
    ANALYSIS_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "AN001",
            "탐지 이력을 찾을 수 없습니다."
    ),

    ANALYSIS_RATE_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "AN002",
            "분석 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."
    ),

    /** 존재하지 않거나 다른 사용자가 소유한 화이트리스트 항목을 동일하게 숨긴다. */
    WHITELIST_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "WL001",
            "화이트리스트 항목을 찾을 수 없습니다."
    ),

    /** 같은 사용자가 정규화 결과가 같은 발신자를 중복 등록하지 못하게 한다. */
    WHITELIST_DUPLICATE(
            HttpStatus.CONFLICT,
            "WL002",
            "이미 화이트리스트에 등록된 발신자입니다."
    ),

    KAKAO_AUTH_FAILED(
            HttpStatus.UNAUTHORIZED,
            "AU015",
            "카카오 인증에 실패했습니다."
    ),

    KAKAO_ALREADY_LINKED(
            HttpStatus.CONFLICT,
            "AU016",
            "이미 다른 카카오 계정과 연동되어 있습니다."
    ),

    KAKAO_SERVER_ERROR(
            HttpStatus.SERVICE_UNAVAILABLE,
            "AU017",
            "카카오 서버와 통신에 실패했습니다."
    ),

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "US001",
            "사용자를 찾을 수 없습니다."
    ),

    ACCOUNT_LOCKED(
            HttpStatus.FORBIDDEN,
            "US002",
            "계정이 잠겼습니다. 휴대폰 인증으로 잠금을 해제해 주세요."
    ),

    DEVICE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "NO001",
            "등록된 기기를 찾을 수 없습니다."
    ),

    FAMILY_INVITE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "FA001",
                    "유효하지 않은 초대 코드입니다."
    ),

    FAMILY_INVITE_EXPIRED(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "FA002",
                    "만료된 초대 코드입니다."
    ),

    FAMILY_LINK_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "FA003",
                    "가족 연결 정보를 찾을 수 없습니다."
    ),

    FAMILY_LINK_FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "FA004",
                    "해당 가족 연결에 접근할 권한이 없습니다."
    ),

    FAMILY_SELF_LINK(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "FA005",
                    "자기 자신과 연결할 수 없습니다."
    ),

    FAMILY_SAFETY_CASE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "FA006",
            "가족 안전 대응 건을 찾을 수 없습니다."
    ),

    FAMILY_SAFETY_INVALID_STATUS(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "FA007",
            "처리할 수 없는 가족 안전 상태입니다."
    ),

    FAMILY_SAFETY_ALREADY_RESOLVED(
            HttpStatus.CONFLICT,
            "FA008",
            "이미 처리가 완료된 가족 안전 대응 건입니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
