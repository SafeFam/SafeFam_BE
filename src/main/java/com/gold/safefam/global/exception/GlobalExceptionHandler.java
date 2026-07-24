package com.gold.safefam.global.exception;

import com.gold.safefam.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 컨트롤러 밖으로 전파된 비즈니스·요청 검증 예외를 공통 ApiResponse 형식으로 변환한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 도메인 ErrorCode에 정의된 HTTP 상태와 메시지로 비즈니스 예외를 응답한다. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getMessage()));
    }

    /** JSON 요청 본문의 Bean Validation 실패를 공통 400 입력 오류로 변환한다. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException() {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getMessage()));
    }

    /** 쿼리·경로 파라미터의 메서드 검증 실패를 공통 400 입력 오류로 변환한다. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException() {
        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getMessage()));
    }
}
