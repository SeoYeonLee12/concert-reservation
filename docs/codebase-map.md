# Concert Reservation — Codebase Map

> 작성 시점: 2026-05-07 (feature/performance 브랜치)
> 목적: AI 어시스턴트 및 신규 기여자가 프로젝트 전체 지형을 빠르게 파악하기 위한 단일 진실 원천. 코드 수정 시 갱신할 것.

## 1. 도메인 진척도

| 도메인 | presentation | application | domain | 비고 |
|--------|-------------|-------------|--------|------|
| **user** | UserController, PointController | UserService, RedisService, PointCharger | User, UserRepository | ✅ 완성 |
| **auth** | AuthController | AuthService, TokenService | Token, Auth, AuthArgumentResolver | ✅ 완성 |
| **performance** | PerformanceController | PerformanceService (@Cacheable 3개) | Performance, Schedule, *Repository | ✅ 조회 완성 |
| **performanceseat** | ❌ | ❌ | PerformanceSeat (@Version), SeatStatus | 🟡 엔티티만 |
| **reservation** | ❌ | ❌ | Reservation, ReservationStatus | 🟡 엔티티만 |
| **pointHistory** | ❌ | ❌ | PointHistory, *Repository, CollectType | 🟡 엔티티만 |
| **place** | ❌ | ❌ | Place, Seat | 🟡 엔티티만 |

## 2. 핵심 엔티티 & 관계

```
Performance ─1:N→ Schedule ─N:1→ Place ─1:N→ Seat
                     │                          │
                     └──1:N→ PerformanceSeat ←──┘   (@Version, 낙관적 락)
                                  │
                              Reservation ─N:1→ User
                                  │
                              PointHistory ─N:1→ User (+ Reservation nullable)
```

- **모든 엔티티** `SoftDeletedDomain` 상속 → `BaseDomain(createdAt, updatedAt)` + `deletedAt`
- **@Version 적용**: `PerformanceSeat` (`performanceseat/domain/PerformanceSeat.java:53`)
- **enum**: `PerformanceStatus(BOOKING/READY/CLOSED)`, `SeatStatus(AVAILABLE/TEMPORARY/SOLD)`, `ReservationStatus(PENDING/CONFIRMED/CANCELLED)`, `CollectType(CHARGE/USE/REFUND)`

## 3. 횡단 관심사 (`global/`)

### AOP
| 어노테이션 | 적용 클래스 | 역할 |
|----------|----------|------|
| `@ExecutionTime` | `global/aop/ExecutionTimer.java` | StopWatch 기반 메서드 실행시간 로깅 |
| `@Retry` | `global/aop/retry/OptimisticLockRetryAspect.java` | OptimisticLockException / StaleObjectStateException 재시도 |

### Config 빈
| 클래스 | 등록 빈 | 비고 |
|------|--------|------|
| `JpaConfig` | `@EnableJpaAuditing` | createdAt/updatedAt 자동화 |
| `RedisTemplateConfig` | StringRedisTemplate, RedisTemplate<String,Object> | Refresh Token + 일반 객체 |
| `CacheConfig` (수정중) | RedisCacheManager (per-cache TTL) | performanceList 5m / Detail 30m / Schedules 1m |
| `RetryConfig` | `@EnableRetry` | Spring Retry 활성화 |
| `AuthConfig` | AuthArgumentResolver 등록 | @Auth 파라미터 해석 |

### 인증 흐름
```
요청(Authorization: Bearer …) → AuthArgumentResolver.resolveArgument
   → BearerTokenExtractor.extract
   → TokenService.extractUserId (Jwts.parser().verifyWith(secretKey))
   → 메서드 파라미터 userId(Long) 주입
```

### 예외 흐름
`도메인 throw GlobalException(ErrorCode)` → `GlobalExceptionHandler` → ErrorCode.httpStatus + ErrorResponse

## 4. 동시성 제어

| 위치 | 기법 | 사용처 |
|------|------|------|
| `UserRepository.findByUsersIdForUpdate` | `@Lock(PESSIMISTIC_WRITE)` | 포인트 충전(`UserService.chargePoint`) |
| `PerformanceSeat.@Version` + `@Retry` | 낙관적 락 + AOP 재시도 | 좌석 예약(서비스 미구현) |

## 5. 인프라 / 모니터링

### Compose 파일
| 파일 | 컨테이너 | 포트 |
|------|----------|------|
| `docker-compose-local.yml` | MySQL 8.0, Redis Alpine, app | 3306 / 6379 / 8080 |
| `test/docker-compose-monitoring.yaml` | Prometheus, Grafana, k6 | 9090 / 3000 |

### Prometheus
- scrape interval 5초
- 타깃: `http://host.docker.internal:8080/actuator/prometheus`
- k6 → Prometheus remote-write 활성

### Grafana
- provisioning: `test/grafana-provisioning/{datasources,dashboards}/`
- dashboard JSON: `test/grafana-dashboard/`

### k6
- `test/k6-scripts/performance-cache-test.js`: VU 50, 30s, p95<500ms

### 프로필 매트릭스
| 프로필 | DB | Redis | DDL |
|--------|-----|-------|-----|
| local | MySQL localhost:3306 | localhost:6379 | validate |
| test | H2 (mem, MySQL mode) | localhost:6379 | create-drop |
| prod | env vars | env vars | env vars |

## 6. 테스트 커버리지 갭

| 도메인 | 단위 | 통합 | 슬라이스 | 합계 |
|--------|------|------|---------|------|
| user | 1 | 2 | 0 | 3 |
| performance | 0 | 0 | 0 | **0** |
| performanceseat | 0 | 0 | 0 | **0** |
| auth | 0 | 0 | 0 | **0** |
| pointHistory | 0 | 0 | 0 | **0** |
| reservation | 0 | 0 | 0 | **0** |

- `.github/workflows/` 없음 → CI 파이프라인 부재
- PerformanceService `@Cacheable` 동작 검증 테스트 0건
- 낙관적 락 재시도 동시성 테스트 0건

## 7. 현재 작업 (feature/performance)

### Uncommitted
| 파일 | 변경 요지 |
|------|----------|
| `global/config/CacheConfig.java` | RedisCacheManager 빈 추가, per-cache TTL |
| `performance/application/PerformanceService.java` | RedisTemplate 의존 제거, @Cacheable 3개 적용, **Thread.sleep(300) 데모 지연 추가** |
| `test/docker-compose-monitoring.yaml` | k6/Prometheus/Grafana 통합 정비 |
| `test/k6-scripts/` (untracked) | performance-cache-test.js 신규 |

### 알려진 정리 항목
1. **PerformanceService** `Thread.sleep(300)` 제거
2. **CacheConfig** Jackson `activateDefaultTyping(EVERYTHING) + allowIfBaseType(Object.class)` → 캐시 도메인 타입(`PerformanceListResult`, `PerformanceGetResult`, `PerformanceScheduleListResult`)으로 좁히기 (역직렬화 RCE 회피)
3. **PerformanceService.findPerformanceById** `@Transactional(readOnly=true)` 누락
4. 캐시 무효화 정책 (`@CacheEvict`) 없음 — 쓰기 경로 추가 시 정의 필요

## 8. 다음 단계 트리거

- **Day 1 마무리**: 위 정리 항목 + 캐시 단위 테스트
- **Day 2 풀세트**: 100만건 더미 데이터 → EXPLAIN before → 인덱스 적용 → EXPLAIN after → k6 4시나리오 부하
- **Day 3 이후**: Redisson 분산 락(좌석 선점), Kafka 대기열, SSE 알림, CI/CD

## 9. 산출물 위치

- 코드맵 (이 문서): `docs/codebase-map.md`
- 포트폴리오 글/ADR/k6 리포트: `/Users/sylee/Documents/concert-reservation-portfolio/`
