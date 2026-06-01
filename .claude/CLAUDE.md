<!-- OMC:START -->
<!-- OMC:VERSION:4.14.1 -->

# oh-my-claudecode - Intelligent Multi-Agent Orchestration

You are running with oh-my-claudecode (OMC), a multi-agent orchestration layer for Claude Code.
Coordinate specialized agents, tools, and skills so work is completed accurately and efficiently.

<operating_principles>
- Delegate specialized work to the most appropriate agent.
- Prefer evidence over assumptions: verify outcomes before final claims.
- Choose the lightest-weight path that preserves quality.
- Consult official docs before implementing with SDKs/frameworks/APIs.
</operating_principles>

<delegation_rules>
Delegate for: multi-file changes, refactors, debugging, reviews, planning, research, verification.
Work directly for: trivial ops, small clarifications, single commands.
Route code to `executor` (use `model=opus` for complex work). Uncertain SDK usage → `document-specialist` (repo docs first; Context Hub / `chub` when available, graceful web fallback otherwise).
</delegation_rules>

<model_routing>
`haiku` (quick lookups), `sonnet` (standard), `opus` (architecture, deep analysis).
Direct writes OK for: `~/.claude/**`, `.omc/**`, `.claude/**`, `CLAUDE.md`, `AGENTS.md`.
</model_routing>

<skills>
Invoke via `/oh-my-claudecode:<name>`. Trigger patterns auto-detect keywords.
Tier-0 workflows include `autopilot`, `ultrawork`, `ralph`, `team`, and `ralplan`.
Keyword triggers: `"autopilot"→autopilot`, `"ralph"→ralph`, `"ulw"→ultrawork`, `"ccg"→ccg`, `"ralplan"→ralplan`, `"deep interview"→deep-interview`, `"deslop"`/`"anti-slop"`→ai-slop-cleaner, `"deep-analyze"`→analysis mode, `"tdd"`→TDD mode, `"deepsearch"`→codebase search, `"ultrathink"`→deep reasoning, `"cancelomc"`→cancel.
Team orchestration is explicit via `/team`.
Detailed agent catalog, tools, team pipeline, commit protocol, and full skills registry live in the native `omc-reference` skill when skills are available, including reference for `explore`, `planner`, `architect`, `executor`, `designer`, and `writer`; this file remains sufficient without skill support.
</skills>

<verification>
Verify before claiming completion. Size appropriately: small→haiku, standard→sonnet, large/security→opus.
If verification fails, keep iterating.
</verification>

<execution_protocols>
Broad requests: explore first, then plan. 2+ independent tasks in parallel. `run_in_background` for builds/tests.
Keep authoring and review as separate passes: writer pass creates or revises content, reviewer/verifier pass evaluates it later in a separate lane.
Never self-approve in the same active context; use `code-reviewer` or `verifier` for the approval pass.
Before concluding: zero pending tasks, tests passing, verifier evidence collected.
</execution_protocols>

<hooks_and_context>
Hooks inject `<system-reminder>` tags. Key patterns: `hook success: Success` (proceed), `[MAGIC KEYWORD: ...]` (invoke skill), `The boulder never stops` (ralph/ultrawork active).
Persistence: `<remember>` (7 days), `<remember priority>` (permanent).
Kill switches: `DISABLE_OMC`, `OMC_SKIP_HOOKS` (comma-separated).
</hooks_and_context>

<cancellation>
`/oh-my-claudecode:cancel` ends execution modes. Cancel when done+verified or blocked. Don't cancel if work incomplete.
</cancellation>

<worktree_paths>
State: `.omc/state/`, `.omc/state/sessions/{sessionId}/`, `.omc/notepad.md`, `.omc/project-memory.json`, `.omc/plans/`, `.omc/research/`, `.omc/logs/`
</worktree_paths>

## Setup

Say "setup omc" or run `/oh-my-claudecode:omc-setup`.

<!-- OMC:END -->

<!-- User customizations (migrated from previous CLAUDE.md) -->
# Concert Reservation

콘서트 예매 시스템 - Spring Boot 기반 백엔드 애플리케이션

## 기술 스택

- Java 21, Spring Boot 3.5.9, Gradle
- MySQL 8.0 (JPA/Hibernate), Redis (토큰 저장 & 캐시)
- JWT 인증 (jjwt 0.12.6), Spring Retry, Lombok
- Micrometer + Prometheus (모니터링)
- 테스트: JUnit 5, Mockito, H2 (인메모리)

## 프로젝트 구조

```
src/main/java/com/example/concertreservation/
├── auth/              # JWT 인증 (토큰 발급/검증, @Auth 리졸버)
├── user/              # 사용자 (회원가입, 로그인, 포인트)
├── performance/       # 공연 (목록/상세 조회, 스케줄)
├── performanceseat/   # 공연 좌석 (가격, 상태, 낙관적 락)
├── reservation/       # 예약
├── pointHistory/      # 포인트 이력 (충전/사용/환불)
├── place/             # 공연장 (장소, 좌석 배치)
└── global/            # 공통 설정 및 유틸리티
    ├── aop/           # ExecutionTimer, OptimisticLockRetryAspect
    ├── config/        # Redis, Cache, JPA, Auth, Retry 설정
    ├── entity/        # BaseDomain, SoftDeletedDomain 베이스 엔티티
    └── exception/     # GlobalException, ErrorCode, ExceptionHandler
```

각 도메인은 `presentation` → `application` → `domain` 레이어 구조를 따른다.

## 빌드 & 실행

```bash
# 로컬 인프라 (MySQL, Redis)
docker compose -f docker-compose-local.yml up -d

# 빌드
./gradlew build

# 실행 (기본 프로필: local)
./gradlew bootRun

# 테스트
./gradlew test
```

## 주요 API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/users/signup` | 회원가입 |
| POST | `/api/users/login` | 로그인 |
| GET | `/api/users` | 사용자 정보 조회 (@Auth) |
| POST | `/api/points` | 포인트 충전 (@Auth) |
| GET | `/api/points` | 포인트 조회 (@Auth) |
| GET | `/api/points/history` | 포인트 이력 조회 (@Auth) |
| POST | `/api/auth` | 토큰 재발급 |
| POST | `/api/auth/logout` | 로그아웃 |
| GET | `/api/performances` | 공연 목록 조회 |
| GET | `/api/performances/{id}` | 공연 상세 조회 |
| GET | `/api/performances/{id}/schedules` | 공연 스케줄 조회 |

## 핵심 설계 패턴

- **동시성 제어**: 포인트 충전은 비관적 락(`PESSIMISTIC_WRITE`), 좌석 예약은 낙관적 락(`@Version` + `@Retry` AOP)
- **Soft Delete**: `SoftDeletedDomain` 베이스 클래스로 `deletedAt` 필드 관리
- **인증**: JWT Access/Refresh 토큰, Refresh 토큰은 Redis에 TTL 저장 (`RT:{userId}`)
- **에러 처리**: `ErrorCode` 인터페이스 → 도메인별 enum 구현 → `GlobalExceptionHandler`에서 일괄 처리

## 설정 프로필

- `local`: MySQL localhost:3306, Redis localhost:6379
- `test`: H2 인메모리 (create-drop), Redis localhost:6379
- `prod`: 환경변수 기반 외부 설정

## 모니터링

- Actuator: `/actuator/prometheus`
- `test/` 디렉토리에 Grafana + Prometheus + k6 대시보드 설정 포함
