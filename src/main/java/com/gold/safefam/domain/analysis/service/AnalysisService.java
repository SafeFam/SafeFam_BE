package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.dto.*;
import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.entity.AnalysisFeedback;
import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import com.gold.safefam.domain.analysis.enums.RiskLevel;
import com.gold.safefam.domain.analysis.mapper.AnalysisResponseMapper;
import com.gold.safefam.domain.analysis.privacy.MessageContentProtector;
import com.gold.safefam.domain.analysis.privacy.MessageContentProtector.ProtectedContent;
import com.gold.safefam.domain.analysis.repository.AnalysisFeedbackRepository;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import com.gold.safefam.global.response.PageResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 문자 분석 생성과 인증 사용자 이력 관리 유스케이스를 조율하는 애플리케이션 서비스.
 * 분석 규칙, 개인정보 처리, DTO 변환, DB 쿼리는 각각의 전용 컴포넌트에 위임한다.
 */
@Service
public class AnalysisService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final MessageContentProtector contentProtector;
    private final AnalysisRepository analysisRepository;
    private final AnalysisFeedbackRepository analysisFeedbackRepository;
    private final AnalysisResponseMapper responseMapper;
    private final AnalysisRequestWriter analysisRequestWriter;

    /** 개인정보 보호·영속화 컴포넌트를 조합한다. */
    public AnalysisService(
            MessageContentProtector contentProtector,
            AnalysisRepository analysisRepository,
            AnalysisFeedbackRepository analysisFeedbackRepository,
            AnalysisResponseMapper responseMapper,
            AnalysisRequestWriter analysisRequestWriter
    ) {
        this.contentProtector = contentProtector;
        this.analysisRepository = analysisRepository;
        this.analysisFeedbackRepository = analysisFeedbackRepository;
        this.responseMapper = responseMapper;
        this.analysisRequestWriter = analysisRequestWriter;
    }

    /* 비동기 분석 요청 접수 후 PENDING으로 저장,
        실제 AI 분석과 결과 반영은 RabbitMQ 기반 후속 처리에서 수행
     */
    public AnalysisAcceptedResponse requestAnalysis(
            Long userId,
            AnalysisRequest request
    ) {
        String clientMessageId =
                normalizeClientMessageId(request.clientMessageId());

        if (clientMessageId != null) {
            Analysis existing = analysisRepository
                    .findByUserIdAndClientMessageId(userId, clientMessageId)
                    .orElse(null);

            if (existing != null) {
                return new AnalysisAcceptedResponse(
                        existing.getId(),
                        existing.getStatus()
                );
            }
        }

        ProtectedContent protectedContent =
                contentProtector.protect(request.content());

        try {
            return analysisRequestWriter.create(
                    userId,
                    clientMessageId,
                    request,
                    protectedContent
            );
        } catch (DataIntegrityViolationException exception) {
            return analysisRepository
                    .findByUserIdAndClientMessageId(
                            userId,
                            clientMessageId
                    )
                    .map(existing -> new AnalysisAcceptedResponse(
                            existing.getId(),
                            existing.getStatus()
                    ))
                    .orElseThrow(() -> exception);
        }
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
        OffsetDateTime fromAt = from != null
                ? toStartOfDay(from)
                : OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime toExclusive = to != null
                ? toStartOfNextDay(to)
                : OffsetDateTime.of(9999, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("receivedAt"), Sort.Order.desc("id"))
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
        return date.atStartOfDay(SERVICE_ZONE).toOffsetDateTime();
    }

    private OffsetDateTime toStartOfNextDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(SERVICE_ZONE).toOffsetDateTime();
    }
}
