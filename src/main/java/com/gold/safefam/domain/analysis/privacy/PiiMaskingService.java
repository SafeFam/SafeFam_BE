package com.gold.safefam.domain.analysis.privacy;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RabbitMQ 페이로드 생성 전 문자 원문의 개인정보를 타입별 토큰으로 치환
 * URL은 VirusTotal 분석에 필요하므로 마스킹에서 제외
 * 마스킹 순서: RRN → CARD → PHONE → ACCOUNT → EMAIL
 * 구체적인 패턴부터 처리해야 오탐(예: 주민번호가 계좌번호로 잘못 마스킹)을 방지 가능
 */
@Component
public class PiiMaskingService {

    // URL: http(s):// 또는 www. 로 시작하는 토큰 (마스킹 제외 대상, 이메일 도메인의 www는 제외)
    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)(?<!@)(?:https?://|www\\.)[A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+;=%]+"
    );

    // 주민번호: 6자리-7자리 (하이픈/공백 구분자 필수, 연속 13자리는 제외)
    private static final Pattern RRN_PATTERN = Pattern.compile(
            "(?<!\\d)\\d{6}[- ]\\d{7}(?!\\d)"
    );

    // 카드번호: 4자리 4묶음(하이픈/공백 허용) 또는 16자리 연속
    private static final Pattern CARD_PATTERN = Pattern.compile(
            "(?<!\\d)(?:\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}|\\d{4}[- ]?\\d{6}[- ]?\\d{5})(?!\\d)"
    );

    // 계좌번호: 하이픈 그룹 형식 또는 10~14자리 연속 숫자
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile(
            "(?<!\\d)\\d{2,6}-\\d{2,6}-\\d{2,6}(?:-\\d{1,6})?(?!\\d)|(?<!\\d)\\d{10,14}(?!\\d)"
    );

    // 전화번호: 0으로 시작, 하이픈/공백 구분 또는 연속 10~11자리
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:0\\d{1,2}[- ]?\\d{3,4}[- ]?\\d{4}|0\\d{9,10})(?!\\d)"
    );

    // 이메일
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(?i)[A-Z0-9._%+\\-]+@[A-Z0-9.\\-]+\\.[A-Z]{2,}"
    );

    // URL 구간은 건너뛰고 나머지 텍스트만 마스킹
    public String mask(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        StringBuilder result = new StringBuilder();
        Matcher urlMatcher = URL_PATTERN.matcher(content);
        int lastEnd = 0;

        while (urlMatcher.find()) {
            result.append(maskPii(content.substring(lastEnd, urlMatcher.start())));
            result.append(urlMatcher.group());
            lastEnd = urlMatcher.end();
        }

        result.append(maskPii(content.substring(lastEnd)));

        return result.toString();
    }

    // RRN → CARD → PHONE → ACCOUNT → EMAIL 순서로 치환
    private String maskPii(String text) {
        return EMAIL_PATTERN.matcher(
                ACCOUNT_PATTERN.matcher(
                        PHONE_PATTERN.matcher(
                                CARD_PATTERN.matcher(
                                        RRN_PATTERN.matcher(text)
                                                .replaceAll("[RRN]")
                                ).replaceAll("[CARD]")
                        ).replaceAll("[PHONE]")
                ).replaceAll("[ACCOUNT]")
        ).replaceAll("[EMAIL]");
    }
}