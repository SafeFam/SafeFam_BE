# 기관 발신번호 후보 검토 절차

## 목적

`institution_sender_candidates`는 실제 분석 요청에서 관찰된 기관명과 숫자형
발신번호를 사람 검토 대상으로 모으는 테이블이다. 반복 관찰은 공식 번호의 증거가
아니므로 어떤 후보도 자동으로 공식 번호로 승인하지 않는다.

## 후보 생성과 상태

- 기관명이 감지되고 숫자형 발신번호를 정규화할 수 있을 때만 후보를 누적한다.
- URL이 없어 `institutionMatch.checked=false`여도 감지된 기관명이 있으면 후보가 된다.
- 기본 3회 관찰 시 `COLLECTING`에서 `REVIEW_REQUIRED`로 전환한다.
- 기준은 `SENDER_CANDIDATE_REVIEW_THRESHOLD` 환경변수로 조정할 수 있다.
- `APPROVED`와 `REJECTED`는 사람 검토 결과이며 추가 관찰로 바뀌지 않는다.

## 검토 순서

1. 검토 대기 후보를 조회한다.

   ```sql
   SELECT id,
          institution,
          normalized_sender,
          observation_count,
          first_seen_at,
          last_seen_at,
          last_analysis_id
   FROM institution_sender_candidates
   WHERE status = 'REVIEW_REQUIRED'
   ORDER BY observation_count DESC, last_seen_at DESC;
   ```

2. 기관 공식 홈페이지, 공식 고객센터 또는 기관이 게시한 문서를 통해 발신번호를
   직접 확인한다. 검색 결과나 수신 횟수만으로 승인하지 않는다.

3. 공식 번호임을 확인하면 승인 상태로 변경한다.

   ```sql
   UPDATE institution_sender_candidates
   SET status = 'APPROVED',
       reviewed_at = CURRENT_TIMESTAMP
   WHERE id = :candidate_id
     AND status = 'REVIEW_REQUIRED';
   ```

4. 공식 번호가 아니거나 기관과 관계없는 번호로 확인되면 거절한다.

   ```sql
   UPDATE institution_sender_candidates
   SET status = 'REJECTED',
       reviewed_at = CURRENT_TIMESTAMP
   WHERE id = :candidate_id
     AND status = 'REVIEW_REQUIRED';
   ```

5. 승인된 번호는 검증한 공식 출처를 PR 설명에 기록하고 SafeFam_AI의
   `OfficialInstitution.known_sender_numbers`에 반영한다.

## 판정 사용 시 주의사항

- `known_sender_numbers`에 없는 번호는 위험 번호가 아니라 미확인 번호다.
- 목록이 충분히 축적되기 전에는 번호 불일치만으로 위험 점수를 올리지 않는다.
- 후보 번호를 애플리케이션 로그나 일반 사용자 API에 노출하지 않는다.
