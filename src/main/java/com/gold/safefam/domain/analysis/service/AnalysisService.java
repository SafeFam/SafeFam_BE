package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.dto.AnalysisFeedbackRequest;
import com.gold.safefam.domain.analysis.dto.AnalysisListItemResponse;
import com.gold.safefam.domain.analysis.dto.AnalysisRequest;
import com.gold.safefam.domain.analysis.dto.AnalysisResponse;
import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.entity.AnalysisFeedback;
import com.gold.safefam.domain.analysis.entity.AnalysisIndicator;
import com.gold.safefam.domain.analysis.entity.AnalysisKeyword;
import com.gold.safefam.domain.analysis.entity.AnalysisUrlRisk;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.mapper.AnalysisResponseMapper;
import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult;
import com.gold.safefam.domain.analysis.privacy.MessageContentProtector;
import com.gold.safefam.domain.analysis.privacy.MessageContentProtector.ProtectedContent;
import com.gold.safefam.domain.analysis.repository.AnalysisFeedbackRepository;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.domain.notification.entity.Device;
import com.gold.safefam.domain.notification.repository.DeviceRepository;
import com.gold.safefam.domain.notification.service.FcmService;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import com.gold.safefam.global.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 문자 분석 생성과 인증 사용자 이력 관리 유스케이스를 조율하는 애플리케이션 서비스.
 * 분석 규칙, 개인정보 처리, DTO 변환, DB 쿼리는 각각의 전용 컴포넌트에 위임한다.
 */
@Service
public class AnalysisService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final MessageRiskAnalyzer riskAnalyzer;
    private final MessageContentProtector contentProtector;
    private final AnalysisRepository analysisRepository;
    private final AnalysisFeedbackRepository analysisFeedbackRepository;
    private final AnalysisResponseMapper responseMapper;
    private final RiskKeywordExtractor keywordExtractor;
    private final DeviceRepository deviceRepository;
    private final FcmService fcmService;

    /** 분석 엔진·개인정보 보호·영속화·키워드 추출·알림 컴포넌트를 조합한다. */
    public AnalysisService(
            MessageRiskAnalyzer riskAnalyzer,
            MessageContentProtector contentProtector,
            AnalysisRepository analysisRepository,
            AnalysisFeedbackRepository analysisFeedbackRepository,
            AnalysisResponseMapper responseMapper,
            RiskKeywordExtractor keywordExtractor,
            DeviceRepository deviceRepository,
            FcmService fcmService
    ) {
        this.riskAnalyzer = riskAnalyzer;
        this.contentProtector = contentProtector;
        this.analysisRepository = analysisRepository;
        this.analysisFeedbackRepository = analysisFeedbackRepository;
        this.responseMapper = responseMapper;
        this.keywordExtractor = keywordExtractor;
        this.deviceRepository = deviceRepository;
        this.fcmService = fcmService;
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
        keywordExtractor.extract(request.content())
                .forEach(keyword -> analysis.addKeyword(new AnalysisKeyword(keyword)));

        Analysis saved = analysisRepository.save(analysis);
        sendPushNotification(userId, saved.getRiskLevel(), saved.getId());
        return responseMapper.toResponse(saved);
    }

    /** 인증 사용자의 분석 이력을 조건에 맞게 최신순으로 조회한다. */
    @Transactional(readOnly = true)
    public PageResponse<AnalysisListItemResponse> getAnalyses(
            Long userId,
            int page,
            int size,
            RiskLevel riskLevel,
            PhishingCategory category,
            LocalDate from,
            LocalDate to
    ) {
        validateDateRange(from, to);
        OffsetDateTime fromAt = toStartOfDay(from);
        OffsetDateTime toExclusive = toStartOfNextDay(to);
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("analyzedAt"), Sort.Order.desc("id"))
        );

        Page<Analysis> result = analysisRepository.search(
                userId,
                riskLevel,
                category,
                fromAt,
                toExclusive,
                pageable
        );
        return new PageResponse<>(
                result.getContent().stream()
                        .map(responseMapper::toListItemResponse)
                        .toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
    }

    /** 상세 응답은 반드시 인증 사용자가 소유한 분석만 반환한다. */
    @Transactional(readOnly = true)
    public AnalysisResponse getAnalysis(Long userId, Long analysisId) {
        return responseMapper.toResponse(getOwnedAnalysis(userId, analysisId));
    }

    /** 분석과 연결된 피드백·탐지 근거·URL 결과를 함께 삭제한다. */
    @Transactional
    public void deleteAnalysis(Long userId, Long analysisId) {
        Analysis analysis = getOwnedAnalysis(userId, analysisId);
        analysisFeedbackRepository.findByAnalysisId(analysisId)
                .ifPresent(analysisFeedbackRepository::delete);
        analysisRepository.delete(analysis);
    }

    /** 분석당 피드백 한 건을 유지하며 재전송하면 기존 값을 갱신한다. */
    @Transactional
    public void submitFeedback(Long userId, Long analysisId, AnalysisFeedbackRequest request) {
        Analysis analysis = getOwnedAnalysis(userId, analysisId);
        AnalysisFeedback feedback = analysisFeedbackRepository.findByAnalysisId(analysisId)
                .map(existing -> {
                    existing.update(request.type(), request.comment());
                    return existing;
                })
                .orElseGet(() -> new AnalysisFeedback(analysis, request.type(), request.comment()));
        analysisFeedbackRepository.save(feedback);
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

    private Analysis getOwnedAnalysis(Long userId, Long analysisId) {
        return analysisRepository.findByIdAndUserId(analysisId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private OffsetDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(SERVICE_ZONE).toOffsetDateTime();
    }

    private OffsetDateTime toStartOfNextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(SERVICE_ZONE).toOffsetDateTime();
    }

    private void sendPushNotification(Long userId, RiskLevel riskLevel, Long analysisId) {
        List<Device> devices = deviceRepository.findByUserId(userId);
        if (devices.isEmpty()) return;

        String title = switch (riskLevel) {
            case HIGH -> "⚠️ 위험 문자 탐지";
            case MEDIUM -> "⚠️ 의심 문자 탐지";
            case LOW -> "✅ 문자 분석 완료";
        };
        String body = switch (riskLevel) {
            case HIGH -> "피싱 위험 문자가 탐지되었습니다. 즉시 확인하세요.";
            case MEDIUM -> "의심스러운 문자가 탐지되었습니다. 확인해 보세요.";
            case LOW -> "분석 결과 안전한 문자로 확인되었습니다.";
        };

        devices.forEach(device -> fcmService.sendNotification(device.getFcmToken(), title, body, analysisId));
    }
}
