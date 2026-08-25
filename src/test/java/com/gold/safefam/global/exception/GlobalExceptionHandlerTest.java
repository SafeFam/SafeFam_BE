package com.gold.safefam.global.exception;

import com.gold.safefam.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessExceptionIncludesDomainErrorCode() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(
                new BusinessException(ErrorCode.ACCOUNT_LOCKED)
        );

        assertEquals(403, response.getStatusCode().value());
        assertEquals("ERROR", response.getBody().status());
        assertEquals("US002", response.getBody().code());
        assertEquals(ErrorCode.ACCOUNT_LOCKED.getMessage(), response.getBody().message());
        assertNull(response.getBody().data());
    }

    @Test
    void invalidInputIncludesCommonErrorCode() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidInput();

        assertEquals(400, response.getStatusCode().value());
        assertEquals("C001", response.getBody().code());
        assertEquals(ErrorCode.INVALID_INPUT.getMessage(), response.getBody().message());
    }

    @Test
    void successResponseKeepsExistingFieldsAndHasNoErrorCode() {
        ApiResponse<String> response = ApiResponse.success("성공", "data");

        assertEquals("SUCCESS", response.status());
        assertNull(response.code());
        assertEquals("성공", response.message());
        assertEquals("data", response.data());
    }
}
