package com.gold.safefam.domain.chat.service;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.entity.AnalysisIndicator;
import com.gold.safefam.domain.analysis.enums.AnalysisSource;
import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.domain.chat.client.AiChatClient;
import com.gold.safefam.domain.chat.client.AiChatClient.AiChatRequest;
import com.gold.safefam.domain.chat.client.AiChatClient.AiChatResponse;
import com.gold.safefam.domain.chat.dto.ChatMessage;
import com.gold.safefam.domain.chat.dto.ChatRequest;
import com.gold.safefam.domain.chat.dto.ChatResponse;
import com.gold.safefam.domain.chat.dto.ChatRole;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    @Test
    void buildsTrustedAnalysisContextAndForwardsMultiTurnMessages() {
        AnalysisRepository analysisRepository = mock(AnalysisRepository.class);
        AiChatClient aiChatClient = mock(AiChatClient.class);
        ChatService chatService = new ChatService(analysisRepository, aiChatClient);
        Analysis analysis = completedAnalysis();

        when(analysisRepository.findWithIndicatorsByIdAndUserId(101L, 7L))
                .thenReturn(Optional.of(analysis));
        when(aiChatClient.chat(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AiChatResponse("공식 기관에 먼저 확인하세요."));

        ChatResponse response = chatService.chat(
                7L,
                new ChatRequest(
                        101L,
                        List.of(
                                new ChatMessage(ChatRole.USER, "어떻게 해야 하나요?"),
                                new ChatMessage(ChatRole.ASSISTANT, "링크를 누르지 마세요."),
                                new ChatMessage(ChatRole.USER, "이미 눌렀어요.")
                        )
                )
        );

        assertEquals("공식 기관에 먼저 확인하세요.", response.message());

        ArgumentCaptor<AiChatRequest> captor = ArgumentCaptor.forClass(AiChatRequest.class);
        verify(aiChatClient).chat(captor.capture());
        AiChatRequest forwarded = captor.getValue();

        assertEquals(92, forwarded.analysisContext().riskScore());
        assertEquals("HIGH", forwarded.analysisContext().riskLevel());
        assertEquals("FINANCIAL_INSTITUTION", forwarded.analysisContext().category());
        assertEquals("금융기관 사칭과 송금 유도가 탐지됐습니다.", forwarded.analysisContext().explanation());
        assertEquals("URGENCY", forwarded.analysisContext().indicators().get(0).type());
        assertEquals("user", forwarded.messages().get(0).role());
        assertEquals("assistant", forwarded.messages().get(1).role());
    }

    @Test
    void hidesAnalysesNotOwnedByAuthenticatedUser() {
        AnalysisRepository analysisRepository = mock(AnalysisRepository.class);
        AiChatClient aiChatClient = mock(AiChatClient.class);
        ChatService chatService = new ChatService(analysisRepository, aiChatClient);

        when(analysisRepository.findWithIndicatorsByIdAndUserId(101L, 7L))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatService.chat(7L, request())
        );

        assertEquals(ErrorCode.ANALYSIS_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void rejectsAnalysisThatIsStillPending() {
        AnalysisRepository analysisRepository = mock(AnalysisRepository.class);
        AiChatClient aiChatClient = mock(AiChatClient.class);
        ChatService chatService = new ChatService(analysisRepository, aiChatClient);
        Analysis pending = Analysis.pending(
                7L,
                "message-101",
                "국민은행",
                "a".repeat(64),
                "분석 대기 문자",
                AnalysisSource.MANUAL,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        when(analysisRepository.findWithIndicatorsByIdAndUserId(101L, 7L))
                .thenReturn(Optional.of(pending));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatService.chat(7L, request())
        );

        assertEquals(ErrorCode.CHAT_ANALYSIS_NOT_READY, exception.getErrorCode());
    }

    private ChatRequest request() {
        return new ChatRequest(
                101L,
                List.of(new ChatMessage(ChatRole.USER, "어떻게 해야 하나요?"))
        );
    }

    private Analysis completedAnalysis() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Analysis analysis = new Analysis(
                7L,
                "message-101",
                "국민은행",
                "a".repeat(64),
                "금융기관 사칭 문자",
                PhishingCategory.FINANCIAL_INSTITUTION,
                AnalysisSource.MANUAL,
                20,
                90,
                92,
                RiskLevel.HIGH,
                "금융기관 사칭과 송금 유도가 탐지됐습니다.",
                now,
                now
        );
        analysis.addIndicator(new AnalysisIndicator(
                IndicatorType.URGENCY,
                "긴급한 송금을 요구합니다."
        ));
        return analysis;
    }
}
