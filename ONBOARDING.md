# Concert Reservation — 온보딩 가이드

> 새 세션 시작 시 이 문서부터 읽어라. 10분 안에 전체 컨텍스트 복구 가능.

---

## 1. 현재 상태 (2026-06-01 기준)

| 항목 | 상태 |
|------|------|
| 현재 브랜치 | `feat/kafka-monitoring` |
| PR #8 | ✅ main 머지 완료 (feat/kafka-domain-event-uuid-idempotency) |
| PR #9 | ✅ develop 머지 완료 (feat/hikari-named-lock-fix) |
| PR #10 | ✅ feat/kafka-monitoring → develop (열려있음) |
| 다음 작업 | lz4 설정 재검토 (현재 구조에서 역효과) → PR #10 머지 |

### 완료된 구현 목록

| 단계 | 내용 |
|------|------|
| Day 1+2 | Cache, 인덱스, Soft Delete, k6 성능 측정 |
| Phase B | 멀티모듈 전환 (5모듈), JWT/Redis/보안 |
| Day 3-1 | cross-context Long ID 약한 참조 |
| Day 3-2 | Outbox Pattern + 결제 확정/환불 |
| Day 3-3 | Cache Stampede 방어, 좌석 만료 스케줄러 |
| Day 3-4 | Redisson 분산 락, 락-트랜잭션 경계 분리 |
| Day 4 | Kafka KRaft 통합 (at-least-once + 멱등성) |
| Day 5 | 입장 대기열 + 락 전략 3종 (Redisson / Named Lock / Optimistic) |
| 2026-05-29 | **DomainEvent 추상화 + UUID 멱등성 키 + 비동기 Kafka 발행** |
| 2026-06-01 세션1~3 | **Outbox 재처리 개선: PRODUCE_FAIL 재처리 + retryCount + ABANDONED + DeadLetter** |
| 2026-06-01 세션4~5 | **HikariCP Named Lock 풀 고갈 구조 개선: DataSource 분리 + Semaphore(4, fair)** |
| 2026-06-02 세션5 | **k6 실측 완료 + PR #9 머지 + 이력서 초안 작성 (대기 큐 + HikariCP 개선)** |
| 2026-06-02 세션6 | **Kafka 모니터링(kafbat/kafka-ui) + 기준선 측정 + 개선 구현(lz4+partition3+concurrency3)** |
| 2026-06-02 세션7 | **lz4 실측(kafka-dump-log.sh): 단일 배치 +4.2% 오버헤드. consumer=Redis 전용(DataSource 분리 불필요). 023 문서 전면 수정** |
| 2026-06-03 세션8 | **lz4 제거(count=1 배치 구조에서 역효과). fetch.max.wait.ms 제거(fetch.min.bytes=1 기본값으로 발동 안 함). 이력서 글 작성(모니터링→병목→개선 흐름)** |
| 2026-06-03 세션9 | **이력서 글 다듬기(파티션/컨슈머 관계 정정, timeout 원인 분리 및 개선 방향 추가). HikariCP pool 고갈 근본 분석. confirm 비동기 처리(202+폴링) 설계 인터뷰 완료** |
| 2026-06-03 세션10 | **confirm 비동기 구현 완료(ReservationAsyncService + 202 + Redis 폴링). k6 100VU 실증: 202 Accepted 100/100, COMPLETED 100/100, 에러 0건, p95=1,985ms. 풀 분리 불필요 확인** |

---

## 2. 환경 설정 (새 환경 시작 시)

### 2-1. docker-compose-local.yml 필수 설정

이 파일은 `.gitignore` 대상이므로 **새 환경에서 수동으로 추가**해야 한다.

```yaml
kafka:
  environment:
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,PLAINTEXT_INTERNAL://0.0.0.0:29092,CONTROLLER://0.0.0.0:9093
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:29092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT

app:
  environment:
    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: 50
```

> **이유**: 앱 컨테이너에서 `localhost:9092`는 자기 자신을 가리킴 → 컨테이너 간 통신은 `kafka:29092` 필수.
> HikariCP 기본 pool=10은 Named Lock(요청당 연결 2개) + 100VU 부하에서 전량 타임아웃 발생.

### 2-2. DB 마이그레이션 (신규 DB 시작 시)

```bash
# test/sql/ 파일을 순서대로 실행
docker exec -i concert-reservation-db mysql -u concert-reservation-user -p123 concert-reservation-db \
  < test/sql/01-dummy-places-seats.sql
# ... 02 ~ 05 순서대로
# 06번은 분리 실행 필요 (MySQL은 ADD COLUMN IF NOT EXISTS 미지원)
docker exec -i concert-reservation-db mysql -u concert-reservation-user -p123 concert-reservation-db \
  -e "CREATE TABLE IF NOT EXISTS waiting_queue (...);"
docker exec -i concert-reservation-db mysql -u concert-reservation-user -p123 concert-reservation-db \
  -e "ALTER TABLE performance_seat MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0;"
```

> 실제 SQL 내용은 `test/sql/06-waiting-queue-version-column.sql` 참조.

---

## 3. 빠른 실행 명령

```bash
# 인프라 전체 시작 (DB, Redis, Kafka, App)
docker compose -f docker-compose-local.yml up -d

# 앱만 재빌드 후 재시작 (코드 변경 시)
./gradlew :app:bootJar && docker compose -f docker-compose-local.yml up --build app -d

# 테스트 실행
./gradlew test

# k6 락 전략 비교 테스트 (Docker)
docker run --rm -v $(pwd)/test/k6-scripts:/scripts \
  -e STRATEGY=redisson -e SEAT_ID=<AVAILABLE_SEAT_ID> \
  grafana/k6 run /scripts/lock-strategy-comparison-test.js
```

> **주의**: k6는 반드시 Docker로 실행. `brew install k6` 사용 금지.
> SEAT_ID는 매 실행마다 새 좌석 사용 (한 번 선점되면 재사용 불가).

---

## 4. 읽어야 할 파일 순서 (컨텍스트 복구용)

### Step 1: 최근 세션 파악
```
/Users/sylee/Documents/concert-reservation-portfolio/HANDOFF-2026-06-03-3.md    ← 가장 최근 (세션10: confirm 비동기 구현 완료 + k6 100VU 실증)
/Users/sylee/Documents/concert-reservation-portfolio/HANDOFF-2026-06-03-2.md    ← 세션9: 이력서 글 다듬기 + confirm 비동기 설계 인터뷰
/Users/sylee/Documents/concert-reservation-portfolio/HANDOFF-2026-06-03.md     ← 세션8: lz4·fetch.max.wait.ms 제거 + 이력서 글
/Users/sylee/Documents/concert-reservation-portfolio/HANDOFF-2026-06-02-3.md   ← 세션7: lz4 실측 + 023 문서 수정
/Users/sylee/Documents/concert-reservation-portfolio/HANDOFF-2026-06-02-2.md   ← 세션6: Kafka 모니터링
/Users/sylee/Documents/concert-reservation-portfolio/HANDOFF-2026-06-02.md     ← 세션5: HikariCP k6 + 이력서
/Users/sylee/Documents/concert-reservation-portfolio/HANDOFF-2026-06-01-4.md
/Users/sylee/Documents/concert-reservation-portfolio/HANDOFF-2026-06-01-3.md
/Users/sylee/Documents/concert-reservation-portfolio/HANDOFF-2026-05-29.md
/Users/sylee/Documents/concert-reservation-portfolio/HANDOFF-2026-05-22-2.md
```

### Step 2: 설계 결정 확인
```
/Users/sylee/Documents/concert-reservation-portfolio/decisions/018-domain-event-uuid-idempotency.md  ← 최신 (2026-05-29)
/Users/sylee/Documents/concert-reservation-portfolio/decisions/017-waiting-queue-and-lock-strategy.md  ← Day 5 핵심
/Users/sylee/Documents/concert-reservation-portfolio/decisions/014-lock-tx-boundary-separation.md
/Users/sylee/Documents/concert-reservation-portfolio/decisions/015-kafka-integration-strategy.md
/Users/sylee/Documents/concert-reservation-portfolio/decisions/016-kafka-consumer-idempotency.md
```

### Step 3: 트러블슈팅 이력
```
/Users/sylee/Documents/concert-reservation-portfolio/troubleshooting/11-waiting-queue-lock-strategy-comparison.md  ← Named Lock Pool 고갈 포함
```

### Step 4: 핵심 소스 코드

| 파일 | 읽어야 하는 이유 |
|------|----------------|
| `context-reservation/src/main/java/.../reservation/application/ReservationService.java` | 락 전략 라우팅 진입점 |
| `context-reservation/src/main/java/.../reservation/application/strategy/ReservationLockStrategy.java` | 전략 인터페이스 |
| `context-reservation/src/main/java/.../reservation/application/strategy/NamedLockReservationStrategy.java` | Named Lock 구현 |
| `context-reservation/src/main/java/.../reservation/application/strategy/RedissonReservationStrategy.java` | Redisson 구현 |
| `context-reservation/src/main/java/.../reservation/application/strategy/OptimisticLockReservationStrategy.java` | 낙관적 락 구현 |
| `context-reservation/src/main/java/.../waiting/domain/WaitingQueue.java` | 대기열 상태 기계 |
| `context-catalog/src/main/java/.../performanceseat/domain/PerformanceSeat.java` | @Version 필드 |
| `common/src/main/java/.../global/event/DomainEvent.java` | Outbox 추상 엔티티 (SINGLE_TABLE + UUID) |
| `common/src/main/java/.../global/kafka/producer/PaymentConfirmedDomainEvent.java` | DomainEvent 구현체 |
| `common/src/main/java/.../global/kafka/producer/PaymentEventListener.java` | @Async Kafka 발행 (AFTER_COMMIT) |
| `common/src/main/java/.../global/kafka/consumer/KafkaIdempotencyChecker.java` | Redis SETNX 멱등성 검사 |
| `common/src/main/java/.../global/kafka/consumer/PaymentKafkaConsumer.java` | UUID 기반 Kafka 소비 |
| `common/src/main/java/.../global/config/AsyncConfig.java` | EVENT_ASYNC_TASK_EXECUTOR 설정 |

---

## 5. 프로젝트 모듈 구조

```
concert-reservation/
├── app/                    # 진입점 (ConcertReservationApplication)
├── common/                 # 공통: JWT, global exception, AOP, base entity
├── context-user/           # 사용자 도메인 (회원가입, 로그인, 포인트)
├── context-catalog/        # 공연 도메인 (performance, performanceseat)
├── context-reservation/    # 예약 도메인 (reservation, waiting queue)
├── test/
│   ├── k6-scripts/         # 부하 테스트 스크립트 + summary JSON
│   ├── sql/                # DB 마이그레이션 SQL (01~06)
│   └── grafana-provisioning/
├── Dockerfile              # 멀티모듈: ARG JAR_FILE=app/build/libs/*.jar
└── docker-compose-local.yml  # gitignore 대상 — 수동 설정 필요
```

---

## 6. k6 실측 결과 (2026-05-22, 100 VU)

| 전략 | 201 성공 | 5xx | avg | p95 | TPS |
|------|---------|-----|-----|-----|-----|
| Redisson | 1 ✅ | 0 ✅ | 822.8 ms | 1168.5 ms | 2.76 |
| Named Lock | 1 ✅ | 0 ✅ | 4400.3 ms | 6078.1 ms | 2.65 |
| **Optimistic** | **1 ✅** | **0 ✅** | **224.3 ms** | **278.1 ms** | **2.79** |

핵심 발견:
- Optimistic이 가장 빠름: 좌석 SOLD 즉시 99건 → 재시도 없이 즉각 409
- Named Lock: 기본 pool=10에서 100VU 전량 타임아웃 → pool=50 필수
- 세 전략 모두 정합성(201=1) + 안전성(5xx=0) 보장

---

## 7. 미완료 작업

### ✅ 완료: confirm 비동기 구현 (세션10)

202 Accepted + Redis 폴링 구현 완료. k6 100VU: 에러 0건, p95=1,985ms.
풀 분리 불필요 (EVENT_ASYNC_TASK_EXECUTOR max=4 그대로 사용).
커밋: 486b753, 0eeb369

### 우선순위 1: 포트폴리오 글 파일 저장

- `kafka-monitoring.md` (세션8-9 확정본): `/Users/sylee/Documents/concert-reservation-portfolio/resume/kafka-monitoring.md` — 아직 미저장
- `confirm-async-polling.md`: `/Users/sylee/Documents/concert-reservation-portfolio/resume/confirm-async-polling.md` — ✅ 저장 완료 (세션10)
- `decisions/024-confirm-async-polling.md` — ✅ 저장 완료 (세션10)

### 우선순위 2: PR #10 머지
```
feat/kafka-monitoring → develop (PR #10 열려있음)
포함 커밋: cdcbe61, 8232ad7, cc80af0, 8c8b888, ae526a0, 486b753, 0eeb369
```

### 우선순위 3: Task 3 — 전체 파일 상세 문서화 (미착수)

각 파일에 대해 아래 3가지를 기록:
1. **무엇을 하는 파일인가** — 한 줄 요약
2. **언제 읽어야 하는가** — 어떤 기능 개발/디버깅 시 참조
3. **왜 이렇게 설계했는가** — 핵심 설계 결정 근거

대상 레이어: domain → application → infrastructure → presentation 순서  
대상 모듈: context-reservation 우선 (가장 복잡도 높음)

---

## 8. 주요 규칙 (이 프로젝트 협업 규칙)

- **인프라는 무조건 Docker**: k6, DB, Redis, Kafka 전부 — `brew install` 사용 금지
- **결정 사항은 반드시 상의 후 진행**
- **포트폴리오 기록 필수**: 새 발견(트러블슈팅, 실측 수치)은 반드시 portfolio 디렉토리에 기록
- **테스트 수치는 전부 기록**: k6 결과, p95, TPS 등 수치 누락 금지
