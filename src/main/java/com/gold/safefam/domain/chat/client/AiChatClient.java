package com.gold.safefam.domain.chat.client;

import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;

/** FastAPI의 stateless /api/chat 계약만 담당하는 HTTP 클라이언트. */
@Component
public class AiChatClient {

    private static final String CHAT_PATH = "/api/chat";

    private final RestTemplate restTemplate;
    private final String chatUrl;

    @Autowired
    public AiChatClient(
            @Value("${safefam.ai.chat.base-url:http://localhost:8000}") String baseUrl,
            @Value("${safefam.ai.chat.connect-timeout:3s}") Duration connectTimeout,
            @Value("${safefam.ai.chat.read-timeout:60s}") Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(requestFactory);
        this.chatUrl = normalizeBaseUrl(baseUrl) + CHAT_PATH;
    }

    AiChatClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.chatUrl = normalizeBaseUrl(baseUrl) + CHAT_PATH;
    }

    public AiChatResponse chat(AiChatRequest request) {
        try {
            AiChatResponse response = restTemplate.postForObject(
                    chatUrl,
                    request,
                    AiChatResponse.class
            );
            if (response == null || response.message() == null || response.message().isBlank()) {
                throw new BusinessException(ErrorCode.CHAT_INVALID_RESPONSE);
            }
            return response;
        } catch (BusinessException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            if (hasSocketTimeoutCause(exception)) {
                throw new BusinessException(ErrorCode.CHAT_SERVICE_TIMEOUT);
            }
            throw new BusinessException(ErrorCode.CHAT_SERVICE_UNAVAILABLE);
        } catch (HttpStatusCodeException exception) {
            HttpStatusCode status = exception.getStatusCode();
            if (status.is5xxServerError()) {
                throw new BusinessException(ErrorCode.CHAT_SERVICE_UNAVAILABLE);
            }
            throw new BusinessException(ErrorCode.CHAT_INVALID_RESPONSE);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.CHAT_INVALID_RESPONSE);
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
    }

    private boolean hasSocketTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public record AiChatRequest(
            AnalysisContext analysisContext,
            List<Message> messages
    ) {
    }

    public record AnalysisContext(
            int riskScore,
            String riskLevel,
            String category,
            String explanation,
            List<Indicator> indicators
    ) {
    }

    public record Indicator(
            String type,
            String description
    ) {
    }

    public record Message(
            String role,
            String content
    ) {
    }

    public record AiChatResponse(String message) {
    }
}
