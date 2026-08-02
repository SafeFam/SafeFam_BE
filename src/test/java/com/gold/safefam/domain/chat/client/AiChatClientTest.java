package com.gold.safefam.domain.chat.client;

import com.gold.safefam.domain.chat.client.AiChatClient.AiChatRequest;
import com.gold.safefam.domain.chat.client.AiChatClient.AiChatResponse;
import com.gold.safefam.domain.chat.client.AiChatClient.AnalysisContext;
import com.gold.safefam.domain.chat.client.AiChatClient.Indicator;
import com.gold.safefam.domain.chat.client.AiChatClient.Message;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiChatClientTest {

    @Test
    void callsFastApiChatContractAndReadsMessage() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        AiChatClient client = new AiChatClient(restTemplate, "http://fastapi-ai:8000/");

        server.expect(requestTo("http://fastapi-ai:8000/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "analysisContext": {
                            "riskScore": 92,
                            "riskLevel": "HIGH",
                            "category": "FINANCIAL_INSTITUTION",
                            "explanation": "금융기관 사칭이 탐지됐습니다.",
                            "indicators": [
                              {"type": "URGENCY", "description": "긴급 송금 요구"}
                            ]
                          },
                          "messages": [
                            {"role": "user", "content": "어떻게 해야 하나요?"}
                          ]
                        }
                        """))
                .andRespond(withSuccess(
                        "{\"message\":\"공식 기관에 확인하세요.\"}",
                        MediaType.APPLICATION_JSON
                ));

        AiChatResponse response = client.chat(request());

        assertEquals("공식 기관에 확인하세요.", response.message());
        server.verify();
    }

    @Test
    void mapsFastApiFailureToStableServiceUnavailableError() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        AiChatClient client = new AiChatClient(restTemplate, "http://fastapi-ai:8000");

        server.expect(requestTo("http://fastapi-ai:8000/api/chat"))
                .andRespond(withServerError());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.chat(request())
        );

        assertEquals(ErrorCode.CHAT_SERVICE_UNAVAILABLE, exception.getErrorCode());
        server.verify();
    }

    @Test
    void rejectsResponseWithoutMessage() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);
        AiChatClient client = new AiChatClient(restTemplate, "http://fastapi-ai:8000");

        server.expect(requestTo("http://fastapi-ai:8000/api/chat"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> client.chat(request())
        );

        assertEquals(ErrorCode.CHAT_INVALID_RESPONSE, exception.getErrorCode());
        server.verify();
    }

    private AiChatRequest request() {
        return new AiChatRequest(
                new AnalysisContext(
                        92,
                        "HIGH",
                        "FINANCIAL_INSTITUTION",
                        "금융기관 사칭이 탐지됐습니다.",
                        List.of(new Indicator("URGENCY", "긴급 송금 요구"))
                ),
                List.of(new Message("user", "어떻게 해야 하나요?"))
        );
    }
}
