package com.gold.safefam.domain.analysis.service;

import com.gold.safefam.domain.analysis.enums.PhishingCategory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 발신자와 문자에서 가장 가까운 피싱 주제를 분류하는 컴포넌트.
 *
 * 위험 점수는 계산하지 않으며 금융기관, 정부기관, 대출, 구직, 배송, 메신저 중
 * 키워드가 가장 많이 일치한 유형만 반환한다. 일치 항목이 없으면 OTHER를 사용한다.
 */
@Component
public class PhishingCategoryClassifier {

    private static final Map<PhishingCategory, Pattern> CATEGORY_PATTERNS = categoryPatterns();

    /** 카테고리별 키워드 일치 횟수를 세어 가장 많이 일치한 유형을 반환한다. */
    public PhishingCategory classify(String analysisText) {
        PhishingCategory selected = PhishingCategory.OTHER;
        int highestMatchCount = 0;

        for (Map.Entry<PhishingCategory, Pattern> entry : CATEGORY_PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(analysisText);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            if (count > highestMatchCount) {
                highestMatchCount = count;
                selected = entry.getKey();
            }
        }
        return selected;
    }

    /** 먼저 등록된 유형이 동점일 때 우선되도록 LinkedHashMap 순서를 유지한다. */
    private static Map<PhishingCategory, Pattern> categoryPatterns() {
        Map<PhishingCategory, Pattern> patterns = new LinkedHashMap<>();
        patterns.put(PhishingCategory.FINANCIAL_INSTITUTION,
                pattern("은행|금융감독원|금감원|카드사|카드|계좌|금융기관"));
        patterns.put(PhishingCategory.GOVERNMENT_AGENCY,
                pattern("검찰|경찰|국세청|건강보험|공단|법원|수사관|검사|공무원"));
        patterns.put(PhishingCategory.LOAN,
                pattern("대출|저금리|대환|한도|신용등급|보증료|상환"));
        patterns.put(PhishingCategory.JOB,
                pattern("채용|구인|알바|아르바이트|재택근무|고수익|업무"));
        patterns.put(PhishingCategory.DELIVERY,
                pattern("택배|배송|운송장|주소지|우체국|반송"));
        patterns.put(PhishingCategory.MESSENGER,
                pattern("카카오톡|카톡|메신저|휴대폰 고장|엄마|아빠|자녀|친구 추가"));
        return Collections.unmodifiableMap(patterns);
    }

    private static Pattern pattern(String expression) {
        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
