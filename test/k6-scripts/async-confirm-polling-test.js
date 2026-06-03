/**
 * 비동기 결제 확정 부하 테스트 (202 Accepted + 폴링)
 *
 * 목적:
 *   EVENT_ASYNC_TASK_EXECUTOR (max=4, queue=100) 공유 사용 시
 *   N명 동시 confirm 요청에서 실제 동작 관찰
 *   ① 큐 포화 / TaskRejectedException(500) 발생 여부
 *   ② 폴링으로 COMPLETED 확인까지 총 소요 시간 분포
 *   ③ COMPLETED / FAILED / TIMEOUT 비율
 *
 * 실행 전 필수:
 *   # AVAILABLE 좌석 확인 (VU_COUNT개 이상 필요)
 *   docker exec concert-reservation-db mysql -uconcert-reservation-user -p123 \
 *     concert-reservation-db -e \
 *     "SELECT performance_seat_id FROM performance_seat WHERE status='AVAILABLE' LIMIT 110;"
 *   # → 조회된 ID를 아래 seatIds 배열에 채워넣기
 *
 * 실행:
 *   docker run --rm -v $(pwd)/test/k6-scripts:/scripts \
 *     -e VU_COUNT=100 \
 *     grafana/k6 run /scripts/async-confirm-polling-test.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

const BASE_URL  = __ENV.BASE_URL  || 'http://host.docker.internal:8080';
const VU_COUNT  = parseInt(__ENV.VU_COUNT || '100');

const POLL_INTERVAL_S  = 0.5;   // 500ms 간격 폴링
const POLL_TIMEOUT_S   = 30;    // 30초 내 미완료 시 TIMEOUT 처리

// ── 커스텀 메트릭 ─────────────────────────────────────
const confirmAccepted   = new Counter('confirm_202');        // 202 수신 (비동기 접수)
const confirmFailed202  = new Counter('confirm_not_202');    // 202 외 실패
const statusCompleted   = new Counter('status_completed');   // COMPLETED 수신
const statusFailed      = new Counter('status_failed');      // FAILED 수신
const statusTimeout     = new Counter('status_timeout');     // 30s 내 미완료
const pollToComplete    = new Trend('poll_to_complete_ms');  // 202→COMPLETED 총 시간

export const options = {
  setupTimeout: '5m',
  scenarios: {
    async_confirm: {
      executor:    'shared-iterations',
      vus:         VU_COUNT,
      iterations:  VU_COUNT,
      maxDuration: '5m',
    },
  },
  thresholds: {
    // 202는 즉시 반환 → p95 < 500ms
    'http_req_duration{name:confirm_post}': ['p(95)<500'],
    // COMPLETED까지 총 소요 시간 p95 < 15s (max=4 스레드 제약 감안)
    'poll_to_complete_ms':                  ['p(95)<15000'],
    // 500 에러(TaskRejectedException) 없어야 함
    confirm_not_202:                        ['count<1'],
  },
};

// ── setup: 사용자 생성 → 포인트 충전 → 좌석 선점 ──────
export function setup() {
  console.log(`[setup] ${VU_COUNT}명 준비 시작...`);
  const results = [];
  const ts = Date.now();

  // AVAILABLE 좌석 ID 목록 (2026-06-03 DB 조회 결과)
  const seatIds = [
    1021261, 1024693, 13180, 18570, 19504, 19853, 32030, 45036, 47034, 62992,
    73646, 78388, 86183, 88365, 97559, 97628, 102167, 109505, 111563, 112260,
    123134, 131246, 131579, 136992, 143655, 147867, 160747, 162472, 171855, 177302,
    194166, 195270, 204232, 223104, 223420, 225360, 232331, 242979, 250460, 251347,
    258443, 291955, 304556, 310429, 321466, 335956, 345027, 358430, 365848, 366526,
    377364, 380315, 380422, 382205, 396958, 401799, 402674, 402887, 410621, 419106,
    423794, 427659, 431885, 445923, 461823, 464084, 466388, 467515, 488654, 499508,
    506423, 512713, 537998, 538629, 540971, 544997, 546604, 548608, 549048, 551002,
    552678, 556134, 559642, 572491, 582207, 603001, 605469, 614454, 623948, 641938,
    644322, 661101, 671120, 676764, 691957, 697213, 703820, 704630, 716306, 736553,
    739635, 741597, 741748, 756463, 765338, 769848, 788226, 796335, 820377, 826491,
  ];

  for (let i = 0; i < VU_COUNT; i++) {
    const email    = `async-test-${ts}-${i}@test.com`;
    const password = 'Test1234!!';

    // 1. 회원가입
    const signupRes = http.post(
      `${BASE_URL}/api/users/signup`,
      JSON.stringify({ email, password, name: `AsyncUser${i}`, nickName: `asyncuser${i}` }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    if (signupRes.status !== 201) {
      console.warn(`[setup] signup 실패 i=${i} status=${signupRes.status}`);
      continue;
    }

    // 2. 로그인
    const loginRes = http.post(
      `${BASE_URL}/api/users/login`,
      JSON.stringify({ email, password }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    const token = loginRes.json('accessToken');
    if (!token) { console.warn(`[setup] login 실패 i=${i}`); continue; }

    const authHeader = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` };

    // 3. 포인트 충전
    http.post(`${BASE_URL}/api/points`, JSON.stringify({ updatePointAmount: 300000 }), { headers: authHeader });

    // 4. 좌석 선점
    const seatId      = seatIds[i % seatIds.length];
    const reserveRes  = http.post(
      `${BASE_URL}/api/reservations`,
      JSON.stringify({ performanceSeatId: seatId }),
      { headers: authHeader },
    );
    if (reserveRes.status !== 201) {
      console.warn(`[setup] 선점 실패 i=${i} seatId=${seatId} status=${reserveRes.status} body=${reserveRes.body}`);
      continue;
    }
    const reservationId = reserveRes.json('reservationId');

    results.push({ token, reservationId, seatId });
    sleep(0.05);
  }

  console.log(`[setup] 완료. 결제 준비 ${results.length}건`);
  return { reservations: results };
}

// ── default: 202 받고 폴링 ────────────────────────────
export default function (data) {
  const idx             = (__VU - 1) % data.reservations.length;
  const { token, reservationId } = data.reservations[idx];
  const authHeader      = { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' };

  // 1. POST /confirm → 202 기대
  const startMs = Date.now();
  const confirmRes = http.post(
    `${BASE_URL}/api/reservations/${reservationId}/confirm`,
    null,
    { headers: authHeader, timeout: '10s', tags: { name: 'confirm_post' } },
  );

  const is202 = check(confirmRes, { '결제 확정 202 Accepted': (r) => r.status === 202 });

  if (!is202) {
    confirmFailed202.add(1);
    console.error(`[VU${__VU}] confirm 실패 reservationId=${reservationId} status=${confirmRes.status} body=${confirmRes.body}`);
    return;
  }
  confirmAccepted.add(1);

  // 2. 폴링: GET /confirm/status → COMPLETED or FAILED 대기
  let finalStatus = 'TIMEOUT';
  const deadlineMs = startMs + POLL_TIMEOUT_S * 1000;

  while (Date.now() < deadlineMs) {
    sleep(POLL_INTERVAL_S);

    const statusRes = http.get(
      `${BASE_URL}/api/reservations/${reservationId}/confirm/status`,
      { headers: authHeader, tags: { name: 'confirm_status_poll' } },
    );

    if (statusRes.status !== 200) { continue; }

    const body = statusRes.json();
    if (!body || !body.status) { continue; }

    if (body.status === 'COMPLETED' || body.status === 'FAILED') {
      finalStatus = body.status;
      break;
    }
  }

  const elapsedMs = Date.now() - startMs;

  if (finalStatus === 'COMPLETED') {
    statusCompleted.add(1);
    pollToComplete.add(elapsedMs);
    check(null, { '폴링 COMPLETED': () => true });
  } else if (finalStatus === 'FAILED') {
    statusFailed.add(1);
    console.error(`[VU${__VU}] FAILED reservationId=${reservationId} elapsed=${elapsedMs}ms`);
  } else {
    statusTimeout.add(1);
    console.error(`[VU${__VU}] TIMEOUT reservationId=${reservationId} elapsed=${elapsedMs}ms`);
  }
}

// ── handleSummary ─────────────────────────────────────
export function handleSummary(data) {
  const ms  = (m, k) => (m && m.values[k] != null ? m.values[k].toFixed(1) : 'N/A');
  const cnt = (m)    => (m ? (m.values['count'] || 0) : 0);

  const confirmDur  = data.metrics['http_req_duration{name:confirm_post}'];
  const pollDur     = data.metrics['http_req_duration{name:confirm_status_poll}'];
  const totalDur    = data.metrics['poll_to_complete_ms'];
  const duration_s  = (data.state.testRunDurationMs / 1000).toFixed(1);

  const completed   = cnt(data.metrics['status_completed']);
  const failed      = cnt(data.metrics['status_failed']);
  const timeout     = cnt(data.metrics['status_timeout']);
  const accepted    = cnt(data.metrics['confirm_202']);
  const notAccepted = cnt(data.metrics['confirm_not_202']);
  const tps         = accepted > 0 ? (accepted / duration_s).toFixed(2) : 'N/A';

  console.log('\n========== 비동기 결제 확정 부하 테스트 결과 ==========');
  console.log(`[환경]`);
  console.log(`  동시 VU 수           : ${VU_COUNT}`);
  console.log(`  테스트 소요 시간     : ${duration_s}s`);
  console.log(`  EVENT_ASYNC_TASK_EXECUTOR: max=4, queue=100 (풀 분리 없음)`);
  console.log('');
  console.log(`[POST /confirm 즉시 응답]`);
  console.log(`  202 Accepted         : ${accepted}건`);
  console.log(`  500 이상 실패        : ${notAccepted}건`);
  console.log(`  처리량(TPS)          : ${tps} req/s`);
  console.log(`  p50                  : ${ms(confirmDur, 'p(50)')} ms`);
  console.log(`  p95                  : ${ms(confirmDur, 'p(95)')} ms`);
  console.log(`  avg                  : ${ms(confirmDur, 'avg')} ms`);
  console.log('');
  console.log(`[폴링 → 최종 상태 확인]`);
  console.log(`  COMPLETED            : ${completed}건`);
  console.log(`  FAILED               : ${failed}건`);
  console.log(`  TIMEOUT (30s 초과)   : ${timeout}건`);
  console.log(`  [202→COMPLETED 시간] avg  : ${ms(totalDur, 'avg')} ms`);
  console.log(`  [202→COMPLETED 시간] p50  : ${ms(totalDur, 'p(50)')} ms`);
  console.log(`  [202→COMPLETED 시간] p95  : ${ms(totalDur, 'p(95)')} ms`);
  console.log(`  [202→COMPLETED 시간] max  : ${ms(totalDur, 'max')} ms`);
  console.log('');
  console.log(`[폴링 상태 조회 API 응답시간]`);
  console.log(`  p50                  : ${ms(pollDur, 'p(50)')} ms`);
  console.log(`  p95                  : ${ms(pollDur, 'p(95)')} ms`);
  console.log('');
  console.log(`[분석 포인트]`);
  console.log(`  ① 500 에러 0건 → TaskRejectedException 없음 (큐 포화 미발생)`);
  console.log(`  ② poll_to_complete p95 → max=4 스레드 처리 지연 수준 확인`);
  console.log(`  ③ TIMEOUT 건수 → 30s 내 처리 불가한 요청 존재 여부`);
  console.log('======================================================\n');

  return {
    '/scripts/summary-async-confirm-polling.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
