package com.gold.safefam.domain.analysis.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 원문 토큰을 그대로 저장하지 않고 개인정보가 될 수 없는 사전 정의 위험 키워드만 추출한다.
 * 같은 문자에서 같은 키워드는 한 번만 집계한다.
 */
@Component
public class RiskKeywordExtractor {

    private static final Map<String, Pattern> KEYWORD_PATTERNS = keywordPatterns();

    /** 원문에서 허용 목록에 포함된 위험 표현만 찾아 표준 키워드 목록으로 반환한다. */
    public List<String> extract(String content) {
        return KEYWORD_PATTERNS.entrySet().stream()
                .filter(entry -> entry.getValue().matcher(content).find())
                .map(Map.Entry::getKey)
                .toList();
    }

    /** 집계 가능한 표준 키워드와 실제 문자에서 찾을 정규식을 순서대로 구성한다. */
    private static Map<String, Pattern> keywordPatterns() {
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        patterns.put("계좌 정지", pattern("계좌(?:가|를|는)?\\s*정지"));
        patterns.put("안전계좌", pattern("안전\\s*계좌"));
        patterns.put("인증번호", pattern("인증\\s*번호"));
        patterns.put("주민등록번호", pattern("주민(?:등록)?\\s*번호"));
        patterns.put("카드번호", pattern("카드\\s*번호"));
        patterns.put("저금리", pattern("저금리"));
        patterns.put("대출", pattern("대출"));
        patterns.put("송금", pattern("송금"));
        patterns.put("이체", pattern("이체"));
        patterns.put("입금", pattern("입금"));
        patterns.put("수수료", pattern("수수료"));
        patterns.put("보증금", pattern("보증금"));
        patterns.put("현금", pattern("현금"));
        patterns.put("압류", pattern("압류"));
        patterns.put("체포", pattern("체포"));
        patterns.put("미납", pattern("미납"));
        patterns.put("택배", pattern("택배"));
        return Collections.unmodifiableMap(patterns);
    }

    /** 한글과 영문 대소문자에 안전하게 사용할 공통 정규식 옵션을 적용한다. */
    private static Pattern pattern(String expression) {
        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
