/**
 * 좌석 선점 락 전략 비교 부하 테스트
 *
 * 목적:
 *   Redisson 분산 락 / MySQL Named Lock / JPA 낙관적 락(@Version) 세 가지 전략을
 *   동일 조건(100 VU, 같은 좌석 동시 선점)에서 비교한다.
 *
 * 검증 가설:
 *   ① 세 전략 모두 정합성 보장: 201 성공 = 1건, 나머지 409
 *   ② 응답 시간(p95) 차이: Redisson < Named Lock ≈ Optimistic
 *   ③ Optimistic Lock: 충돌 시 재시도로 인해 TPS는 낮고 지연은 높을 수 있음
 *
 * 실행 방법:
 *   # 전략별 개별 실행
 *   docker run --rm -v $(pwd)/test/k6-scripts:/scripts \
 *     -e STRATEGY=redisson \
 *     grafana/k6 run /scripts/lock-strategy-comparison-test.js
 *
 *   docker run --rm -v $(pwd)/test/k6-scripts:/scripts \
 *     -e STRATEGY=named-lock \
 *     grafana/k6 run /scripts/lock-strategy-comparison-test.js
 *
 *   docker run --rm -v $(pwd)/test/k6-scripts:/scripts \
 *     -e STRATEGY=optimistic \
 *     grafana/k6 run /scripts/lock-strategy-comparison-test.js
 *
 * 주의: Named Lock은 MySQL에서만 동작 (H2 미지원)
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

const BASE_URL  = __ENV.BASE_URL  || 'http://host.docker.internal:8080';
const VU_COUNT  = parseInt(__ENV.VU_COUNT || '100');
const STRATEGY  = __ENV.STRATEGY || 'redisson';

// ── 커스텀 메트릭 ──────────────────────────────────────
const reserveSuccess = new Counter('reserve_success');  // 201 선점 성공
const reserveFailed  = new Counter('reserve_failed');   // 409 선점 충돌
const reserveError   = new Counter('reserve_error');    // 5xx 오류

export const options = {
  setupTimeout: '5m',
  scenarios: {
    lock_comparison: {
      executor:    'shared-iterations',
      vus:         VU_COUNT,
      iterations:  VU_COUNT,
      maxDuration: '3m',
    },
  },
  thresholds: {
    // 세 전략 모두 500 에러 0건이어야 함
    reserve_error:    ['count==0'],
    // 201 성공은 정확히 1건 (동일 좌석 동시 선점)
    reserve_success:  ['count<=1'],
    // p95 3초 이내
    'http_req_duration{name:reserve}': ['p(95)<3000'],
  },
};

// ── setup: 단일 공유 좌석 + VU_COUNT명 사용자 준비 ──────
export function setup() {
  console.log(`[setup] 전략: ${STRATEGY}, VU: ${VU_COUNT}`);
  const ts = Date.now();

  // 공유 좌석 ID (단일 좌석으로 100 VU 충돌 테스트)
  const SHARED_SEAT_ID = parseInt(__ENV.SEAT_ID || '1');

  const users = [];
  for (let i = 0; i < VU_COUNT; i++) {
    const email    = `lock-test-${STRATEGY}-${ts}-${i}@test.com`;
    const password = 'Test1234!!';

    const signupRes = http.post(
      `${BASE_URL}/api/users/signup`,
      JSON.stringify({ email, password, name: `LockUser${i}`, nickName: `lockuser${STRATEGY}${i}` }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    if (signupRes.status !== 201) {
      console.warn(`[setup] signup 실패 i=${i} status=${signupRes.status}`);
      continue;
    }

    const loginRes = http.post(
      `${BASE_URL}/api/users/login`,
      JSON.stringify({ email, password }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    const token = loginRes.json('accessToken');
    if (!token) continue;

    users.push({ token });
    sleep(0.05);
  }

  // 좌석 초기화 (AVAILABLE 상태 보장)
  console.log(`[setup] 완료: ${users.length}명, 좌석 ID: ${SHARED_SEAT_ID}`);
  return { users, seatId: SHARED_SEAT_ID };
}

// ── default: VU_COUNT명이 동일 좌석에 동시 선점 시도 ────
export default function (data) {
  const idx   = (__VU - 1) % data.users.length;
  const token = data.users[idx].token;

  const res = http.post(
    `${BASE_URL}/api/reservations?strategy=${STRATEGY}`,
    JSON.stringify({ performanceSeatId: data.seatId }),
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type':  'application/json',
      },
      timeout: '10s',
      tags:    { name: 'reserve' },
    },
  );

  const is201 = check(res, { '201 선점 성공': (r) => r.status === 201 });
  const is409 = check(res, { '409 선점 충돌': (r) => r.status === 409 });

  if (res.status === 201) {
    reserveSuccess.add(1);
  } else if (res.status === 409) {
    reserveFailed.add(1);
  } else {
    reserveError.add(1);
    console.error(`[VU${__VU}] 예상치 못한 status=${res.status} body=${res.body}`);
  }
}

// ── handleSummary: 전략별 비교 결과 출력 ────────────────
export function handleSummary(data) {
  const dur     = data.metrics['http_req_duration{name:reserve}'];
  const reqs    = data.metrics['http_reqs'];
  const succ    = data.metrics['reserve_success'];
  const failed  = data.metrics['reserve_failed'];
  const err     = data.metrics['reserve_error'];

  const ms  = (m, k) => (m && m.values[k] != null ? m.values[k].toFixed(1) : 'N/A');
  const cnt = (m)    => (m ? m.values['count'] : 0);

  const successCount = cnt(succ);
  const failedCount  = cnt(failed);
  const errorCount   = cnt(err);
  const duration_s   = (data.state.testRunDurationMs / 1000).toFixed(1);
  const tps          = (cnt(reqs) / duration_s).toFixed(2);

  console.log(`\n========== 락 전략 비교: ${STRATEGY.toUpperCase()} ==========`);
  console.log(`[환경]`);
  console.log(`  전략                : ${STRATEGY}`);
  console.log(`  동시 VU 수          : ${VU_COUNT}`);
  console.log(`  총 요청 수          : ${cnt(reqs)}`);
  console.log(`  테스트 소요 시간    : ${duration_s}s`);
  console.log('');
  console.log(`[결과]`);
  console.log(`  201 선점 성공       : ${successCount}건  ← 정확히 1건이어야 함`);
  console.log(`  409 선점 충돌       : ${failedCount}건`);
  console.log(`  5xx 오류            : ${errorCount}건   ← 반드시 0건`);
  console.log(`  처리량(TPS)         : ${tps} req/s`);
  console.log('');
  console.log(`[응답 시간 (reserve 엔드포인트만)]`);
  console.log(`  avg                 : ${ms(dur, 'avg')} ms`);
  console.log(`  p50                 : ${ms(dur, 'p(50)')} ms`);
  console.log(`  p95                 : ${ms(dur, 'p(95)')} ms`);
  console.log(`  p99                 : ${ms(dur, 'p(99)')} ms`);
  console.log(`  max                 : ${ms(dur, 'max')} ms`);
  console.log('');
  console.log(`[검증]`);
  console.log(`  정합성 (201=1)      : ${successCount === 1 ? '✅ PASS' : '❌ FAIL — ' + successCount + '건'}`);
  console.log(`  안전성 (5xx=0)      : ${errorCount === 0 ? '✅ PASS' : '❌ FAIL — ' + errorCount + '건'}`);
  console.log('==============================================\n');

  return {
    [`/scripts/summary-lock-${STRATEGY}.json`]: JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
