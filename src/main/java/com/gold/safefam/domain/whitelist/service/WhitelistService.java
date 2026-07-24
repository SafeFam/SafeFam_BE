package com.gold.safefam.domain.whitelist.service;

import com.gold.safefam.domain.whitelist.dto.WhitelistCheckResponse;
import com.gold.safefam.domain.whitelist.dto.WhitelistRequest;
import com.gold.safefam.domain.whitelist.dto.WhitelistResponse;
import com.gold.safefam.domain.whitelist.entity.WhitelistEntry;
import com.gold.safefam.domain.whitelist.repository.WhitelistRepository;
import com.gold.safefam.global.exception.BusinessException;
import com.gold.safefam.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** 사용자별 화이트리스트 등록과 분석 전 프리패스 확인을 처리한다. */
@RequiredArgsConstructor
@Service
public class WhitelistService {

    private final WhitelistRepository whitelistRepository;

    /** 발신자를 정규화하고 사용자별 중복을 검사한 뒤 새 항목을 저장한다. */
    @Transactional
    public WhitelistResponse create(Long userId, WhitelistRequest request) {
        String sender = normalizeSender(request.sender());
        if (whitelistRepository.findByUserIdAndSender(userId, sender).isPresent()) {
            throw new BusinessException(ErrorCode.WHITELIST_DUPLICATE);
        }

        WhitelistEntry saved = whitelistRepository.save(new WhitelistEntry(
                userId,
                sender,
                normalizeLabel(request.label())
        ));
        return toResponse(saved);
    }

    /** 사용자의 전체 화이트리스트 엔티티를 API 응답 목록으로 변환한다. */
    @Transactional(readOnly = true)
    public List<WhitelistResponse> getAll(Long userId) {
        return whitelistRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** 입력 발신자를 등록 시점과 동일한 규칙으로 정규화해 프리패스 여부를 반환한다. */
    @Transactional(readOnly = true)
    public WhitelistCheckResponse check(Long userId, String sender) {
        String normalizedSender = normalizeSender(sender);
        return new WhitelistCheckResponse(
                normalizedSender,
                whitelistRepository.findByUserIdAndSender(userId, normalizedSender).isPresent()
        );
    }

    /** 사용자 소유권이 확인된 항목만 삭제하고 다른 사용자의 항목은 숨긴다. */
    @Transactional
    public void delete(Long userId, Long whitelistId) {
        WhitelistEntry entry = whitelistRepository.findByIdAndUserId(whitelistId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WHITELIST_NOT_FOUND));
        whitelistRepository.delete(entry);
    }

    /** 영속 엔티티가 외부로 직접 노출되지 않도록 응답 DTO로 변환한다. */
    private WhitelistResponse toResponse(WhitelistEntry entry) {
        return new WhitelistResponse(
                entry.getId(),
                entry.getSender(),
                entry.getLabel(),
                entry.getCreatedAt()
        );
    }

    /**
     * 전화번호는 숫자만 남기고 국제번호 82를 국내 형식으로 바꾼다.
     * 문자형 발신자 ID는 대소문자 차이로 중복되지 않도록 대문자로 정규화한다.
     */
    private String normalizeSender(String sender) {
        String trimmed = sender == null ? "" : sender.trim();
        if (trimmed.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (trimmed.matches("[+\\d()\\-\\s]+")) {
            String digits = trimmed.replaceAll("\\D", "");
            if (digits.startsWith("82") && digits.length() >= 10) {
                digits = "0" + digits.substring(2);
            }
            if (digits.isBlank() || digits.length() > 100) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            return digits;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    /** 공백 라벨은 저장하지 않고 값이 있으면 앞뒤 공백을 제거한다. */
    private String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        return label.trim();
    }
}
