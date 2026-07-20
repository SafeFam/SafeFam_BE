package com.gold.safefam.domain.analysis.privacy;

import com.gold.safefam.domain.analysis.privacy.MessageContentProtector.ProtectedContent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 원문이 해시와 마스킹 미리보기 외의 형태로 저장 단계에 넘어가지 않는지 검증한다. */
class MessageContentProtectorTest {

    private final MessageContentProtector protector = new MessageContentProtector();

    @Test
    void sameContentProducesSameSha256Hash() {
        ProtectedContent first = protector.protect("같은 문자 내용");
        ProtectedContent second = protector.protect("같은 문자 내용");

        assertEquals(first.hash(), second.hash());
        assertEquals(64, first.hash().length());
        assertNotEquals("같은 문자 내용", first.hash());
    }

    @Test
    void previewMasksUrlPhoneEmailAndLongNumber() {
        ProtectedContent result = protector.protect(
                "연락처 010-1234-5678, test@example.com, 계좌 1234567890, https://example.com/login"
        );

        assertTrue(result.preview().contains("010-****-****"));
        assertTrue(result.preview().contains("***@***"));
        assertTrue(result.preview().contains("******"));
        assertTrue(result.preview().contains("[URL]"));
        assertFalse(result.preview().contains("010-1234-5678"));
        assertFalse(result.preview().contains("https://example.com"));
    }

    @Test
    void blankContentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> protector.protect(" "));
    }
}
