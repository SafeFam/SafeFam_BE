package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.dto.AnalysisRequest;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse;
import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.entity.AnalysisIndicator;
import com.gold.safefam.domain.analysis.entity.AnalysisUrlRisk;
import com.gold.safefam.domain.analysis.mapper.AnalysisResponseMapper;
import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult;
import com.gold.safefam.domain.analysis.privacy.MessageContentProtector;
import com.gold.safefam.domain.analysis.privacy.MessageContentProtector.ProtectedContent;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 문자 분석 생성 유스케이스의 순서만 조율하는 애플리케이션 서비스.
 * 분석 규칙, 개인정보 처리, DTO 변환, DB 쿼리는 각각의 전용 컴포넌트에 위임한다.
 */
@Service
public class AnalysisService {

    private final MessageRiskAnalyzer riskAnalyzer;
    private final MessageContentProtector contentProtector;
    private final AnalysisRepository analysisRepository;
    private final AnalysisResponseMapper responseMapper;

    public AnalysisService(
            MessageRiskAnalyzer riskAnalyzer,
            MessageContentProtector contentProtector,
            AnalysisRepository analysisRepository,
            AnalysisResponseMapper responseMapper
    ) {
        this.riskAnalyzer = riskAnalyzer;
        this.contentProtector = contentProtector;
        this.analysisRepository = analysisRepository;
        this.responseMapper = responseMapper;
    }

    /** 인증 사용자 기준으로 중복 확인, 분석, 원문 보호, 저장, 응답 변환을 수행한다. */
    @Transactional
    public AnalysisResponse analyze(Long userId, AnalysisRequest request) {
        String clientMessageId = normalizeClientMessageId(request.clientMessageId());
        if (clientMessageId != null) {
            AnalysisResponse existing = analysisRepository
                    .findByUserIdAndClientMessageId(userId, clientMessageId)
                    .map(responseMapper::toResponse)
                    .orElse(null);
            if (existing != null) {
                return existing;
            }
        }

        MessageRiskAnalysisResult result = riskAnalyzer.analyze(request.sender(), request.content());
        ProtectedContent protectedContent = contentProtector.protect(request.content());
        Analysis analysis = createAnalysis(userId, clientMessageId, request, result, protectedContent);

        result.indicators().forEach(indicator -> analysis.addIndicator(
                new AnalysisIndicator(indicator.type(), indicator.description())
        ));
        result.urls().forEach(url -> analysis.addUrlRisk(
                new AnalysisUrlRisk(url.originalUrl(), url.shortened(), url.suspicious())
        ));

        return responseMapper.toResponse(analysisRepository.save(analysis));
    }

    private Analysis createAnalysis(
            Long userId,
            String clientMessageId,
            AnalysisRequest request,
            MessageRiskAnalysisResult result,
            ProtectedContent protectedContent
    ) {
        return new Analysis(
                userId,
                clientMessageId,
                request.sender(),
                protectedContent.hash(),
                protectedContent.preview(),
                result.category(),
                request.source(),
                result.urlScore(),
                result.patternScore(),
                result.riskScore(),
                result.riskLevel(),
                result.explanation(),
                request.receivedAt(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private String normalizeClientMessageId(String clientMessageId) {
        if (clientMessageId == null || clientMessageId.isBlank()) {
            return null;
        }
        return clientMessageId.trim();
    }
}
