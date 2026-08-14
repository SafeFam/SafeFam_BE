package com.gold.safefam.domain.analysis.service;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

/** 국내 숫자형 SMS 발신번호를 후보 집계에 사용할 동일 형식으로 정규화한다. */
@Component
public class SenderNumberNormalizer {

    private static final Pattern SUPPORTED_FORMAT = Pattern.compile(
            "^(?:\\+|00)?[0-9()\\-\\s]+$"
    );

    public Optional<String> normalize(String sender) {
        if (sender == null || sender.isBlank()) {
            return Optional.empty();
        }

        String trimmed = sender.trim();
        if (!SUPPORTED_FORMAT.matcher(trimmed).matches()) {
            return Optional.empty();
        }

        String digits = trimmed.replaceAll("\\D", "");
        if (trimmed.startsWith("+82") && digits.startsWith("82")) {
            digits = "0" + digits.substring(2);
        } else if (trimmed.startsWith("0082")
                && digits.startsWith("0082")) {
            digits = "0" + digits.substring(4);
        }

        if (digits.length() < 3 || digits.length() > 11) {
            return Optional.empty();
        }

        return Optional.of(digits);
    }
}
