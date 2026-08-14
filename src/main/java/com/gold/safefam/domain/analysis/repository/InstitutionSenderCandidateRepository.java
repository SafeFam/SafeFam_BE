package com.gold.safefam.domain.analysis.repository;

import com.gold.safefam.domain.analysis.entity.InstitutionSenderCandidate;
import com.gold.safefam.domain.analysis.enums.SenderCandidateStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface InstitutionSenderCandidateRepository
        extends JpaRepository<InstitutionSenderCandidate, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InstitutionSenderCandidate>
    findByInstitutionAndNormalizedSender(
            String institution,
            String normalizedSender
    );

    List<InstitutionSenderCandidate> findByStatusOrderByLastSeenAtDesc(
            SenderCandidateStatus status
    );
}
