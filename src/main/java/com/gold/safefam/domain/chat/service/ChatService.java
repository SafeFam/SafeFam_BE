package com.gold.safefam.domain.chat.service;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.enums.AnalysisStatus;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.domain.chat.client.AiChatClient;
import com.gold.safefam.domain.chat.client.AiChatClient.AiChatRequest;
import com.gold.safefam.domain.chat.client.AiChatClient.AnalysisContext;
import com.gold.safefam.domain.chat.client.AiChatClient.Indicator;
import com.gold.safefam.domain.chat.client.AiChatClient.Message;
import com.gold.safefam.domain.chat.dto.ChatRequest;
import com.gold.safefam.domain.chat.dto.ChatResponse;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/** 인증 사용자의 분석 결과를 신뢰 가능한 AI 상담 컨텍스트로 변환한다. */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final AnalysisRepository analysisRepository;
    private final AiChatClient aiChatClient;

    public ChatResponse chat(Long userId, ChatRequest request) {
        AiChatClient.AnalysisContext analysisContext = null;

        if (request.analysisId() != null) {
            Analysis analysis = analysisRepository.findWithIndicatorsByIdAndUserId(
                            request.analysisId(),
                            userId
                    )
                    .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));

            validateChatContext(analysis);
            analysisContext = toAnalysisContext(analysis);
        }

        AiChatClient.AiChatResponse response = aiChatClient.chat(new AiChatRequest(
                analysisContext,
                request.messages().stream()
                        .map(message -> new Message(
                                message.role().name().toLowerCase(Locale.ROOT),
                                message.content()
                        ))
                        .toList()
        ));

        return new ChatResponse(response.message());
    }

    private AnalysisContext toAnalysisContext(Analysis analysis) {
        List<Indicator> indicators = analysis.getIndicators().stream()
                .map(indicator -> new Indicator(
                        indicator.getType().name(),
                        indicator.getDescription()
                ))
                .toList();

        return new AnalysisContext(
                analysis.getTotalScore(),
                analysis.getRiskLevel().name(),
                analysis.getCategory().name(),
                analysis.getExplanation(),
                indicators
        );
    }

    private void validateChatContext(Analysis analysis) {
        boolean successful = analysis.getStatus() == AnalysisStatus.COMPLETED
                || analysis.getStatus() == AnalysisStatus.PARTIAL_SUCCESS;
        boolean hasRequiredResult = analysis.getTotalScore() != null
                && analysis.getRiskLevel() != null
                && analysis.getCategory() != null
                && analysis.getExplanation() != null
                && !analysis.getExplanation().isBlank();

        if (!successful || !hasRequiredResult) {
            throw new BusinessException(ErrorCode.CHAT_ANALYSIS_NOT_READY);
        }
    }
}
