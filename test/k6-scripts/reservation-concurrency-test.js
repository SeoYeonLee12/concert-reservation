/**
 * 분산 락 동시성 부하 테스트
 *
 * 목적: 100명의 사용자가 동일 좌석에 동시 예약 시도 시
 *       Redisson 분산 락이 정확히 1개만 성공시키는지 검증한다.
 *
 * 예상 결과:
 *   - 201 Created  : 1건  (분산 락 → 단 1개 선점 성공)
 *   - 409 Conflict : ~99건 (NOT_RESERVABLE: 이미 선점된 좌석)
 *   - 408 Timeout  : 0~소수 (SEAT_LOCK_TIMEOUT: 락 대기 3s 초과)
 *
 * 실행 전 준비:
 *   # 좌석 상태 AVAILABLE로 초기화
 *   docker exec concert-reservation-db mysql -u concert-reservation-user -p123 \
 *     concert-reservation-db -e \
 *     "UPDATE performance_seat SET status='AVAILABLE', version=version+1 \
 *      WHERE performance_seat_id=${SEAT_ID};"
 *
 * 실행 방법:
 *   docker run --rm -v $(pwd)/test/k6-scripts:/scripts \
 *     -e SEAT_ID=2 \
 *     grafana/k6 run /scripts/reservation-concurrency-test.js
 */
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const SEAT_ID  = parseInt(__ENV.SEAT_ID  || '2');
const VU_COUNT = 100;

const successCount = new Counter('reservation_success');   // 201
const conflictCount = new Counter('reservation_conflict'); // 409
const timeoutCount  = new Counter('reservation_timeout');  // 408

export const options = {
  setupTimeout: '3m', // BCrypt 12라운드: signup당 ~400ms × 100명 = ~80초 필요
  scenarios: {
    concurrent_reservation: {
      executor: 'shared-iterations',
      vus: VU_COUNT,
      iterations: VU_COUNT, // VU당 정확히 1회 → 100 동시 요청
      maxDuration: '5m',
    },
  },
  thresholds: {
    // 5xx 서버 에러는 허용하지 않음 (409/408은 예상된 실패이므로 http_req_failed 제외)
    checks: ['rate>0.95'],
  },
};

/**
 * setup(): 100명의 테스트 계정 생성 및 JWT 토큰 수집.
 * 각 VU가 고유 계정으로 경쟁 — 단일 사용자 중복 방지.
 */
export function setup() {
  console.log(`[setup] ${VU_COUNT}명 계정 생성 시작...`);
  const tokens = [];
  const ts = Date.now();

  for (let i = 0; i < VU_COUNT; i++) {
    const email    = `lock-test-${ts}-${i}@concurrent.test`;
    const password = 'Test1234!!';
    const name     = `LockTest${i}`;
    const nickName = `locktest${i}`;

    // 회원가입
    const signupRes = http.post(
      `${BASE_URL}/api/users/signup`,
      JSON.stringify({ email, password, name, nickName }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    if (signupRes.status !== 201) {
      console.warn(`[setup] signup 실패 i=${i} status=${signupRes.status}`);
    }

    // 로그인
    const loginRes = http.post(
      `${BASE_URL}/api/users/login`,
      JSON.stringify({ email, password }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    const token = loginRes.json('accessToken');
    if (!token) {
      console.warn(`[setup] login 토큰 없음 i=${i}`);
    }
    tokens.push(token);
  }

  console.log(`[setup] 완료. 토큰 ${tokens.filter(Boolean).length}개 준비`);
  return { tokens, seatId: SEAT_ID };
}

/**
 * default: 각 VU가 자신의 JWT로 동일 좌석에 예약 1회 시도.
 * shared-iterations executor → 100 VU가 거의 동시에 실행.
 */
export default function (data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length];

  const res = http.post(
    `${BASE_URL}/api/reservations`,
    JSON.stringify({ performanceSeatId: data.seatId }),
    {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      timeout: '15s',
      tags: { name: 'reservation_try' },
    },
  );

  const ok = check(res, {
    '201 또는 예상된 실패(409/408)': (r) => [201, 409, 408].includes(r.status),
  });

  if (res.status === 201)      { successCount.add(1); }
  else if (res.status === 409) { conflictCount.add(1); }
  else if (res.status === 408) { timeoutCount.add(1); }
  else {
    console.error(`[VU${__VU}] 예상 외 응답: ${res.status} ${res.body}`);
  }
}

export function handleSummary(data) {
  const dur  = data.metrics['http_req_duration'];
  const reqs = data.metrics['http_reqs'];
  const succ = data.metrics['reservation_success'];
  const conf = data.metrics['reservation_conflict'];
  const tout = data.metrics['reservation_timeout'];

  const p95 = dur && dur.values['p(95)'] != null ? dur.values['p(95)'].toFixed(1) : 'N/A';
  const p99 = dur && dur.values['p(99)'] != null ? dur.values['p(99)'].toFixed(1) : 'N/A';

  console.log('\n========== 분산 락 동시성 테스트 결과 ==========');
  console.log(`대상 좌석 ID  : ${SEAT_ID}`);
  console.log(`동시 VU 수    : ${VU_COUNT}`);
  console.log(`총 요청 수    : ${reqs  ? reqs.values['count'] : 'N/A'}`);
  console.log(`201 성공      : ${succ  ? succ.values['count'] : 0} ← 정확히 1이어야 함`);
  console.log(`409 충돌      : ${conf  ? conf.values['count'] : 0}`);
  console.log(`408 타임아웃  : ${tout  ? tout.values['count'] : 0}`);
  console.log(`p95 응답시간  : ${p95} ms`);
  console.log(`p99 응답시간  : ${p99} ms`);
  console.log('================================================\n');

  return {
    '/scripts/summary-reservation-concurrency.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
