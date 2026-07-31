package com.gold.safefam.domain.analysis.privacy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiMaskingServiceTest {

    private PiiMaskingService piiMaskingService;

    @BeforeEach
    void setUp() {
        piiMaskingService = new PiiMaskingService();
    }

    // ── 전화번호 ──────────────────────────────────────────────

    @Test
    @DisplayName("하이픈 전화번호를 마스킹한다")
    void maskPhone_hyphen() {
        assertThat(piiMaskingService.mask("010-1234-5678로 연락하세요"))
                .contains("[PHONE]")
                .doesNotContain("010-1234-5678");
    }

    @Test
    @DisplayName("공백 전화번호를 마스킹한다")
    void maskPhone_space() {
        assertThat(piiMaskingService.mask("010 1234 5678로 연락하세요"))
                .contains("[PHONE]")
                .doesNotContain("010 1234 5678");
    }

    @Test
    @DisplayName("연속 전화번호를 마스킹한다")
    void maskPhone_continuous() {
        assertThat(piiMaskingService.mask("01012345678로 연락하세요"))
                .contains("[PHONE]")
                .doesNotContain("01012345678");
    }

    // ── 계좌번호 ──────────────────────────────────────────────

    @Test
    @DisplayName("하이픈 계좌번호를 마스킹한다")
    void maskAccount_hyphen() {
        assertThat(piiMaskingService.mask("110-1234-567890 계좌로 이체하세요"))
                .contains("[ACCOUNT]")
                .doesNotContain("110-1234-567890");
    }

    @Test
    @DisplayName("연속 계좌번호를 마스킹한다")
    void maskAccount_continuous() {
        assertThat(piiMaskingService.mask("100123456789 계좌로 이체하세요"))  // 12자리
                .contains("[ACCOUNT]")
                .doesNotContain("100123456789");
    }

    // ── 카드번호 ──────────────────────────────────────────────

    @Test
    @DisplayName("하이픈 카드번호를 마스킹한다")
    void maskCard_hyphen() {
        assertThat(piiMaskingService.mask("1234-5678-9012-3456 카드 정지"))
                .contains("[CARD]")
                .doesNotContain("1234-5678-9012-3456");
    }

    @Test
    @DisplayName("연속 16자리 카드번호를 마스킹한다")
    void maskCard_continuous() {
        assertThat(piiMaskingService.mask("1234567890123456 카드 정지"))
                .contains("[CARD]")
                .doesNotContain("1234567890123456");
    }

    // ── 주민번호 ──────────────────────────────────────────────

    @Test
    @DisplayName("하이픈 주민번호를 마스킹한다")
    void maskRrn_hyphen() {
        assertThat(piiMaskingService.mask("900101-1234567 확인 요망"))
                .contains("[RRN]")
                .doesNotContain("900101-1234567");
    }

    // ── 이메일 ──────────────────────────────────────────────

    @Test
    @DisplayName("이메일을 마스킹한다")
    void maskEmail() {
        assertThat(piiMaskingService.mask("test@example.com으로 연락하세요"))
                .contains("[EMAIL]")
                .doesNotContain("test@example.com");
    }

    // ── URL 미마스킹 ──────────────────────────────────────────────

    @Test
    @DisplayName("URL은 마스킹하지 않는다")
    void noMaskUrl_https() {
        String result = piiMaskingService.mask("https://safe.example.com 확인하세요");
        assertThat(result).contains("https://safe.example.com");
    }

    @Test
    @DisplayName("URL 뒤에 붙은 전화번호는 마스킹한다")
    void maskPhone_afterUrl() {
        String result = piiMaskingService.mask("https://safe.example.com 010-1234-5678로 연락");
        assertThat(result)
                .contains("https://safe.example.com")
                .contains("[PHONE]")
                .doesNotContain("010-1234-5678");
    }

    @Test
    @DisplayName("이메일 도메인의 www가 URL로 잘못 마스킹되지 않는다")
    void maskEmail_withWwwDomain() {
        assertThat(piiMaskingService.mask("alice@www.example.com으로 연락하세요"))
                .contains("[EMAIL]")
                .doesNotContain("alice@www.example.com");
    }

    // ── 복합 케이스 ──────────────────────────────────────────────

    @Test
    @DisplayName("계좌번호와 전화번호가 동시에 포함된 경우 둘 다 마스킹한다")
    void maskMultiple() {
        String result = piiMaskingService.mask("110-1234-567890으로 이체 후 010-1234-5678로 연락");
        assertThat(result)
                .contains("[ACCOUNT]")
                .contains("[PHONE]")
                .doesNotContain("110-1234-567890")
                .doesNotContain("010-1234-5678");
    }

    @Test
    @DisplayName("주민번호가 계좌번호 패턴으로 잘못 마스킹되지 않는다")
    void maskRrn_notMaskedAsAccount() {
        String result = piiMaskingService.mask("900101-1234567 확인 요망");
        assertThat(result)
                .contains("[RRN]")
                .doesNotContain("[ACCOUNT]");
    }

    // ── 방어 처리 ──────────────────────────────────────────────

    @Test
    @DisplayName("빈 문자열은 그대로 반환한다")
    void maskEmpty() {
        assertThat(piiMaskingService.mask("")).isEqualTo("");
    }

    @Test
    @DisplayName("null은 그대로 반환한다")
    void maskNull() {
        assertThat(piiMaskingService.mask(null)).isNull();
    }

    @Test
    @DisplayName("PII가 없는 일반 문자는 그대로 반환한다")
    void maskNoPii() {
        String text = "오늘 저녁 메뉴 뭐야?";
        assertThat(piiMaskingService.mask(text)).isEqualTo(text);
    }
}