package com.gold.safefam.domain.analysis.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SenderNumberNormalizerTest {

    private final SenderNumberNormalizer normalizer =
            new SenderNumberNormalizer();

    @Test
    void normalizesDomesticAndInternationalKoreanNumbers() {
        assertEquals(
                "01012345678",
                normalizer.normalize("010-1234-5678").orElseThrow()
        );
        assertEquals(
                "01012345678",
                normalizer.normalize("+82 (10) 1234-5678").orElseThrow()
        );
        assertEquals(
                "01012345678",
                normalizer.normalize("0082-10-1234-5678").orElseThrow()
        );
    }

    @Test
    void acceptsRepresentativeAndShortInstitutionNumbers() {
        assertEquals(
                "15889999",
                normalizer.normalize("1588-9999").orElseThrow()
        );
        assertEquals(
                "1332",
                normalizer.normalize("1332").orElseThrow()
        );
        assertEquals(
                "125",
                normalizer.normalize("125").orElseThrow()
        );
    }

    @Test
    void rejectsNamesAndUnsupportedLengths() {
        assertTrue(normalizer.normalize("국민은행").isEmpty());
        assertTrue(normalizer.normalize("KB1588-9999").isEmpty());
        assertTrue(normalizer.normalize("12").isEmpty());
        assertTrue(normalizer.normalize("010123456789").isEmpty());
    }
}
