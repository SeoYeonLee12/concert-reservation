---
name: project-status
description: 콘서트 예매 프로젝트 현재 진행 상황, 완료된 커밋, 다음 작업 목록
metadata:
  type: project
---

## 프로젝트 위치
- 코드: `/Users/sylee/personal/concert-reservation`
- 브랜치: `feature/kafka` (main PR 머지 완료 — 모든 작업 main에 반영됨)
- 포트폴리오 문서: `/Users/sylee/Documents/concert-reservation-portfolio/`
- 최신 핸드오프: `HANDOFF-2026-05-19.md`

## 모듈 구조
```
:app → :context-reservation → {:context-user, :context-catalog} → :common
```

## 완료된 커밋 (main 기준, 최신순)
```
af49df0 feat: Kafka 이벤트 발행/소비 통합 (Day 4)           ← 최신 (main)
1719d0a fix:  분산 락-트랜잭션 경계 분리 (레이스 컨디션 제거)
dcd20d2 test: Day 3-4 분산 락 동시성 k6 + outbox_event DDL
adbce8a feat: Cache Stampede k6 테스트
1d83db4 feat: DISP-04 좌석 배치도 endpoint
d139623 feat: 좌석 만료 스케줄러 + Cache Stampede 구현
d7ad36f feat: 결제 확정/환불 + Outbox Pattern
a89f685 refactor: cross-context entity Long ID
b372261 feat: 좌석 선점 분산 락
1424bcd security: JWT denylist
15b61e6 security: BCrypt(12) 마이그레이션
daea0b5 security: 환경변수 비밀 관리
754d541 perf: Pageable + @BatchSize 38× 개선
a16ff44 chore: 멀티모듈 전환
```

## 핵심 정량 수치
- list p95: 3.02s → 80ms (**38×**)
- cache-warm p95: 13ms (**220×**)
- EXPLAIN rows: 1,020,544 → 34,342 (**≈30×**)
- Cache Stampede sync=true: 에러 34건 → **0건**
- 동시성 100 VU 동일 좌석: 201×**1**, 409×99, 500×**0**
- Kafka end-to-end: partition=0, offset=0 발행→수신 확인

## 인프라 상태
- MySQL 8.0 (3306) — 실행 중
- Redis alpine (6379) — 실행 중
- Kafka apache/kafka:3.8.1 KRaft (9092) — 실행 중
- Topic: `payment.confirmed`

## 포트폴리오 문서
- troubleshooting: 01~07 완료
- decisions: 001~015 완료 (000-INDEX.md 포함)
- README.md 최종 갱신 완료

## 남은 작업 후보
1. SeatExpirationScheduler 버그 수정 (Optional → List)
2. Kafka Consumer 멱등성 처리
3. 포트폴리오 최종 점검

## 알려진 이슈
- 앱 통합 테스트 2건 실패 (pre-existing, H2/JPA)
- SeatExpirationScheduler: PENDING 복수 결과 충돌 (테스트 잔여 데이터 정리로 임시 해소)

**Why:** 포트폴리오 프로젝트 (주니어 입사 지원용).
**How to apply:** 의사결정은 반드시 사용자와 상의.
