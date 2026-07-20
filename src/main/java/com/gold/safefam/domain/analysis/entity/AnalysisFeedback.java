package com.gold.safefam.domain.analysis.entity;

import com.gold.safefam.domain.analysis.enums.FeedbackType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/** 사용자 피드백을 분석 이력당 한 건만 보관하며 재전송 시 최신 값으로 갱신한다. */
@Getter
@Entity
@Table(name = "analysis_feedbacks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detection_log_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Analysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 30)
    private FeedbackType type;

    @Column(length = 500)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public AnalysisFeedback(Analysis analysis, FeedbackType type, String comment) {
        this.analysis = analysis;
        this.type = type;
        this.comment = normalizeComment(comment);
    }

    /** 같은 분석에 피드백을 다시 제출하면 행을 추가하지 않고 유형과 의견을 교체한다. */
    public void update(FeedbackType type, String comment) {
        this.type = type;
        this.comment = normalizeComment(comment);
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }
}
