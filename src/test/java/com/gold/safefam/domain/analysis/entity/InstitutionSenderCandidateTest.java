package com.gold.safefam.domain.analysis.entity;

import com.gold.safefam.domain.analysis.enums.SenderCandidateStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstitutionSenderCandidateTest {

    @Test
    void approvedCandidateRemainsApprovedWhenObservedAgain() {
        OffsetDateTime now = OffsetDateTime.of(
                2026, 8, 14, 9, 0, 0, 0, ZoneOffset.UTC
        );
        InstitutionSenderCandidate candidate =
                new InstitutionSenderCandidate(
                        "국민은행",
                        "15889999",
                        101L,
                        now,
                        3
                );
        candidate.approve(now.plusHours(1));

        candidate.observe(102L, now.plusHours(2), 3);

        assertEquals(2, candidate.getObservationCount());
        assertEquals(SenderCandidateStatus.APPROVED, candidate.getStatus());
        assertEquals(now.plusHours(1), candidate.getReviewedAt());
    }

    @Test
    void rejectedCandidateRemainsRejectedWhenObservedAgain() {
        OffsetDateTime now = OffsetDateTime.of(
                2026, 8, 14, 9, 0, 0, 0, ZoneOffset.UTC
        );
        InstitutionSenderCandidate candidate =
                new InstitutionSenderCandidate(
                        "국민은행",
                        "15889999",
                        101L,
                        now,
                        1
                );
        candidate.reject(now.plusHours(1));

        candidate.observe(102L, now.plusHours(2), 1);

        assertEquals(SenderCandidateStatus.REJECTED, candidate.getStatus());
    }
}
