package com.gold.safefam.domain.analysis.privacy;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * 분석에 사용한 문자 원문이 DB에 남지 않도록 해시와 마스킹 미리보기를 만드는 컴포넌트.
 * 위험 분석은 원문으로 수행하지만 저장 직전에 이 결과로 치환한다.
 */
@Component
public class MessageContentProtector {

    private static final int PREVIEW_MAX_LENGTH = 120;
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)[^\\s<>\\\"']+"
    );
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)(01[016789])[- ]?\\d{3,4}[- ]?\\d{4}(?!\\d)"
    );
    private static final Pattern LONG_NUMBER_PATTERN = Pattern.compile(
            "(?<!\\d)\\d{6,}(?!\\d)"
    );
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /** 원문 비교용 SHA-256 해시와 화면 표시용 마스킹 미리보기를 함께 반환한다. */
    public ProtectedContent protect(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content must not be blank");
        }
        return new ProtectedContent(hash(content), createPreview(content));
    }

    private String hash(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    private String createPreview(String content) {
        // URL과 직접 식별 가능한 연락처·이메일·긴 숫자를 먼저 가린 후 길이를 제한한다.
        String preview = URL_PATTERN.matcher(content).replaceAll("[URL]");
        preview = EMAIL_PATTERN.matcher(preview).replaceAll("***@***");
        preview = PHONE_PATTERN.matcher(preview).replaceAll("$1-****-****");
        preview = LONG_NUMBER_PATTERN.matcher(preview).replaceAll("******");
        preview = WHITESPACE_PATTERN.matcher(preview).replaceAll(" ").trim();

        if (preview.length() <= PREVIEW_MAX_LENGTH) {
            return preview;
        }
        return preview.substring(0, PREVIEW_MAX_LENGTH - 3) + "...";
    }

    /** DB 저장에 허용되는 원문 대체 값. */
    public record ProtectedContent(String hash, String preview) {
    }
}
