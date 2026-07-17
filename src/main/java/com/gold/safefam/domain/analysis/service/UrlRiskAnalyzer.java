package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.enums.IndicatorType;
import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult.RiskIndicator;
import com.gold.safefam.domain.analysis.model.MessageRiskAnalysisResult.UrlRisk;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 문자 본문에서 URL을 추출하고 주소 형태만으로 위험 신호를 평가하는 컴포넌트.
 *
 * 외부 URL 평판 API를 호출하지 않으므로 악성 여부를 확정하지 않는다. 단축 주소,
 * HTTP, IP 주소, 국제화 도메인, 로그인 유도 경로를 휴리스틱 점수로 반환한다.
 */
@Component
public class UrlRiskAnalyzer {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)[^\\s<>\\\"']+"
    );
    private static final Pattern IPV4_HOST_PATTERN = Pattern.compile(
            "^(?:\\d{1,3}\\.){3}\\d{1,3}$"
    );
    private static final Pattern CREDENTIAL_PATH_PATTERN = Pattern.compile(
            "(?i)(?:login|verify|auth|secure|account|signin|인증|로그인|본인확인)"
    );
    private static final Set<String> SHORTENER_HOSTS = Set.of(
            "bit.ly", "tinyurl.com", "t.co", "url.kr", "han.gl", "me2.kr", "vo.la", "c11.kr"
    );

    /** 문자 안의 모든 URL을 평가하고 가장 높은 URL 점수를 대표 점수로 반환한다. */
    public UrlAnalysis analyze(String content) {
        Matcher matcher = URL_PATTERN.matcher(content);
        List<UrlRisk> urls = new ArrayList<>();
        List<RiskIndicator> indicators = new ArrayList<>();
        Set<IndicatorType> detectedTypes = new LinkedHashSet<>();
        int highestScore = 0;

        while (matcher.find()) {
            String originalUrl = trimTrailingPunctuation(matcher.group());
            UrlEvaluation evaluation = evaluate(originalUrl);
            highestScore = Math.max(highestScore, evaluation.score());
            urls.add(new UrlRisk(
                    originalUrl,
                    evaluation.shortened(),
                    evaluation.suspicious(),
                    evaluation.reasons()
            ));

            addUrlIndicators(evaluation, indicators, detectedTypes);
        }

        // URL이 여러 개면 반복적인 외부 이동 유도를 반영하되 최대 20점까지만 추가한다.
        if (urls.size() > 1) {
            highestScore += Math.min(20, (urls.size() - 1) * 5);
        }
        return new UrlAnalysis(clamp(highestScore), urls, indicators, detectedTypes);
    }

    /** URL 하나를 URI로 해석해 외부 조회 없이 확인 가능한 위험 특성을 점수화한다. */
    private UrlEvaluation evaluate(String originalUrl) {
        // www 주소는 URI 파싱을 위해 임시 HTTPS 스킴을 붙이되 결과에는 원래 주소를 유지한다.
        String parseTarget = originalUrl.regionMatches(true, 0, "www.", 0, 4)
                ? "https://" + originalUrl
                : originalUrl;
        List<String> reasons = new ArrayList<>();
        int score = 0;
        boolean shortened = false;

        try {
            URI uri = new URI(parseTarget);
            String host = uri.getHost();
            String normalizedHost = host == null ? "" : host.toLowerCase(Locale.ROOT);

            if (SHORTENER_HOSTS.contains(normalizedHost)) {
                shortened = true;
                score += 40;
                reasons.add("단축 URL 서비스가 사용되었습니다.");
            }
            if ("http".equalsIgnoreCase(uri.getScheme())) {
                score += 10;
                reasons.add("암호화되지 않은 HTTP 주소입니다.");
            }
            if (IPV4_HOST_PATTERN.matcher(normalizedHost).matches()) {
                score += 40;
                reasons.add("도메인 이름 대신 IP 주소를 사용합니다.");
            }
            if (normalizedHost.contains("xn--")) {
                score += 25;
                reasons.add("유사 도메인에 악용될 수 있는 국제화 도메인입니다.");
            }
            if (CREDENTIAL_PATH_PATTERN.matcher(parseTarget).find()) {
                score += 20;
                reasons.add("로그인 또는 본인 인증을 유도하는 경로가 포함되어 있습니다.");
            }
        } catch (URISyntaxException exception) {
            score = 50;
            reasons.add("정상적으로 해석하기 어려운 URL 형식입니다.");
        }

        int normalizedScore = clamp(score);
        return new UrlEvaluation(shortened, normalizedScore >= 25, normalizedScore, reasons);
    }

    /** URL 평가를 API에 노출할 탐지 근거 enum과 사용자 설명으로 변환한다. */
    private void addUrlIndicators(
            UrlEvaluation evaluation,
            List<RiskIndicator> indicators,
            Set<IndicatorType> detectedTypes
    ) {
        if (evaluation.shortened() && detectedTypes.add(IndicatorType.SHORTENED_URL)) {
            indicators.add(new RiskIndicator(
                    IndicatorType.SHORTENED_URL,
                    "최종 목적지를 바로 확인하기 어려운 단축 URL이 포함되어 있습니다."
            ));
        }
        if (evaluation.suspicious() && detectedTypes.add(IndicatorType.MALICIOUS_URL)) {
            indicators.add(new RiskIndicator(
                    IndicatorType.MALICIOUS_URL,
                    "로그인 유도, IP 주소 사용 등 위험 특성이 있는 URL이 포함되어 있습니다."
            ));
        }
    }

    private static String trimTrailingPunctuation(String url) {
        int end = url.length();
        while (end > 0 && ").,]}>\"'".indexOf(url.charAt(end - 1)) >= 0) {
            end--;
        }
        return url.substring(0, end);
    }

    private static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    /** URL 분석기가 점수 정책과 결과 생성 단계에 전달하는 중간 결과. */
    public record UrlAnalysis(
            int score,
            List<UrlRisk> urls,
            List<RiskIndicator> indicators,
            Set<IndicatorType> detectedTypes
    ) {
        public UrlAnalysis {
            urls = List.copyOf(urls);
            indicators = List.copyOf(indicators);
            detectedTypes = Set.copyOf(detectedTypes);
        }
    }

    /** 외부에 노출하지 않는 URL 한 건의 내부 점수 계산 결과. */
    private record UrlEvaluation(
            boolean shortened,
            boolean suspicious,
            int score,
            List<String> reasons
    ) {
    }
}
