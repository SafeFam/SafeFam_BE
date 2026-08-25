package com.gold.safefam.global.exception;

import com.gold.safefam.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @RestController
    static class ThrowingController {
        @GetMapping("/test/unhandled")
        void boom() {
            throw new IllegalStateException("boom");
        }

        @PostMapping("/test/body")
        void body(@RequestBody Map<String, Object> body) {
        }
    }

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(handler)
            .build();

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

    @Test
    void unexpectedExceptionReturnsCommonErrorShape() throws Exception {
        mockMvc.perform(get("/test/unhandled"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("C999"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void malformedJsonReturnsInvalidInput() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"broken\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력입니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
