package com.gold.safefam.domain.report.service;

import com.gold.safefam.domain.analysis.entity.Analysis;
import com.gold.safefam.domain.analysis.repository.AnalysisRepository;
import com.gold.safefam.domain.report.dto.ReportRequest;
import com.gold.safefam.domain.report.dto.ReportResponse;
import com.gold.safefam.domain.report.entity.PhishingReport;
import com.gold.safefam.domain.report.repository.PhishingReportRepository;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 소유권을 확인한 뒤 원문 없이 익명 신고 스냅샷을 저장한다. */
@RequiredArgsConstructor
@Service
public class ReportService {

    private final AnalysisRepository analysisRepository;
    private final PhishingReportRepository reportRepository;

    /** 분석 소유권과 기존 신고를 확인한 뒤 익명 신고를 생성하거나 기존 값을 반환한다. */
    @Transactional
    public SubmissionResult submit(Long userId, Long analysisId, ReportRequest request) {
        Analysis analysis = analysisRepository.findByIdAndUserId(analysisId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_FOUND));

        PhishingReport existing = reportRepository.findByAnalysisId(analysisId).orElse(null);
        if (existing != null) {
            return new SubmissionResult(toResponse(existing, analysisId), false);
        }

        PhishingReport saved = reportRepository.save(new PhishingReport(analysis, request.type()));
        return new SubmissionResult(toResponse(saved, analysisId), true);
    }

    /** 신고 엔티티에서 외부 노출에 필요한 분류·시각 정보만 응답으로 변환한다. */
    private ReportResponse toResponse(PhishingReport report, Long analysisId) {
        return new ReportResponse(
                report.getId(),
                analysisId,
                report.getType(),
                report.getCategory(),
                report.getRiskLevel(),
                report.getCreatedAt()
        );
    }

    /** 컨트롤러가 신규 생성과 멱등 재요청의 HTTP 상태를 구분하도록 전달하는 내부 결과다. */
    public record SubmissionResult(ReportResponse response, boolean created) {
    }
}
