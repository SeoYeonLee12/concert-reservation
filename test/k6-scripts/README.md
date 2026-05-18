# k6 부하 테스트 시나리오

## 시나리오 목록

| 파일 | 목적 | VU | Duration |
|------|------|----|----------|
| `01-list-no-cache.js` | 캐시 미사용 baseline — `?bust=VU-ITER` + `Cache-Control: no-cache`로 캐시 무효화, p95 측정 | 100 | 1m |
| `02-list-cache-warm.js` | 캐시 hit baseline — 동일 path 반복 호출로 Redis 캐시 적중률 극대화, p95 측정 | 100 | 1m |
| `03-detail-load.js` | 단일 공연 조회 — `/api/performances/{1~1000}` 랜덤 ID, 인덱스 효과 측정 | 100 | 1m |
| `04-schedules-load.js` | 스케줄 조회 — `/api/performances/{1~1000}/schedules` 랜덤 ID, schedule join 쿼리 + 인덱스 효과 측정 | 100 | 1m |

> 기존 `performance-cache-test.js`는 빠른 smoke test (VU 50, 30s) 용도로 유지.

---

## 실행 전 준비

```bash
# 1. 로컬 인프라 (MySQL + Redis)
docker compose -f docker-compose-local.yml up -d

# 2. 모니터링 스택 (Prometheus + Grafana)
docker compose -f test/docker-compose-monitoring.yaml up -d

# 3. 애플리케이션 실행
./gradlew bootRun
```

---

## 실행 순서 (권장)

캐시 효과 비교를 위해 no-cache → cache-warm 순으로 실행한다.

```bash
# 1) 캐시 비활성 baseline
docker run --rm -v $(pwd)/test/k6-scripts:/scripts grafana/k6 run /scripts/01-list-no-cache.js

# 2) 캐시 warm (직전 시나리오로 캐시가 이미 채워진 상태)
docker run --rm -v $(pwd)/test/k6-scripts:/scripts grafana/k6 run /scripts/02-list-cache-warm.js

# 3) 단일 공연 상세 조회
docker run --rm -v $(pwd)/test/k6-scripts:/scripts grafana/k6 run /scripts/03-detail-load.js

# 4) 스케줄 조회
docker run --rm -v $(pwd)/test/k6-scripts:/scripts grafana/k6 run /scripts/04-schedules-load.js
```

BASE_URL 환경변수를 오버라이드하려면:

```bash
docker run --rm -v $(pwd)/test/k6-scripts:/scripts \
  -e BASE_URL=http://host.docker.internal:8080 \
  grafana/k6 run /scripts/01-list-no-cache.js
```

---

## 주목해야 할 메트릭

| 메트릭 | 설명 | 임계값 |
|--------|------|--------|
| `http_req_duration{p(95)}` | 95백분위 응답시간 — 캐시 유무 비교의 핵심 지표 | < 500ms |
| `http_reqs` (TPS) | 초당 처리 요청 수 — 캐시 hit 시 no-cache 대비 유의미한 상승 기대 | — |
| `http_req_failed` | 오류율 — 1% 미만 유지 확인 | < 1% |
| `checks` | check 통과율 | > 99% |

**비교 포인트**

- `01` vs `02`: p95 차이 → 캐시 효과 정량화
- `03` vs `04`: p95 차이 → schedule join 쿼리 오버헤드 측정

---

## summary.json 위치

각 시나리오 종료 후 컨테이너 내 `/scripts/` 에 저장된다.  
호스트에서는 마운트 경로인 `test/k6-scripts/` 아래에서 확인한다.

```
test/k6-scripts/
├── summary-list-no-cache.json
├── summary-list-cache-warm.json
├── summary-detail-load.json
└── summary-schedules-load.json
```
