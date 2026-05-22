/**
 * Kafka 연동 결제 확정 부하 테스트
 *
 * 목적:
 *   결제 확정 API(POST /api/reservations/{id}/confirm)를 N명이 동시 호출할 때
 *   ① API 응답 시간 및 처리량(TPS)
 *   ② Kafka 메시지 발행 손실 여부 (요청 수 == 수신 로그 수)
 *   ③ 비동기 발행(@TransactionalEventListener)이 API 지연에 영향 없음을 확인
 *
 * 흐름:
 *   setup()  → 사용자 생성 → 포인트 충전 → 좌석 선점(PENDING 예약 생성)
 *   default() → 각 VU가 자신의 reservationId로 결제 확정
 *   handleSummary() → 응답 시간 / TPS / Kafka 예상 메시지 수 출력
 *
 * 실행 방법:
 *   docker run --rm -v $(pwd)/test/k6-scripts:/scripts \
 *     -e VU_COUNT=20 \
 *     grafana/k6 run /scripts/kafka-payment-confirmation-test.js
 *
 * 실행 전 좌석 상태 확인 (AVAILABLE 필요):
 *   docker exec concert-reservation-db mysql -uconcert-reservation-user -p123 \
 *     concert-reservation-db -e \
 *     "SELECT performance_seat_id, status FROM performance_seat WHERE status='AVAILABLE' LIMIT 30;"
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

const BASE_URL  = __ENV.BASE_URL  || 'http://host.docker.internal:8080';
const VU_COUNT  = parseInt(__ENV.VU_COUNT || '20');

// ── 커스텀 메트릭 ──────────────────────────────────────
const confirmSuccess = new Counter('confirm_success');   // 200 결제 확정 성공
const confirmFailed  = new Counter('confirm_failed');    // 그 외 실패
const kafkaExpected  = new Counter('kafka_expected');    // 성공 수 == Kafka 발행 기대 수

export const options = {
  setupTimeout: '5m',  // BCrypt 12라운드: ~400ms × VU_COUNT
  scenarios: {
    kafka_confirm: {
      executor:   'shared-iterations',
      vus:        VU_COUNT,
      iterations: VU_COUNT,
      maxDuration: '3m',
    },
  },
  thresholds: {
    http_req_duration:                 ['p(95)<3000'],  // 결제 확정 p95 < 3s
    'http_req_duration{name:confirm}': ['p(95)<2000'],  // 결제 확정만 따로 p95 < 2s
    http_req_failed:                   ['rate<0.01'],   // 에러율 1% 미만
    confirm_success:                   [`count>=${VU_COUNT - 1}`], // 거의 전원 성공
  },
};

// ── setup: 사용자 생성 → 포인트 충전 → 좌석 선점 ───────
export function setup() {
  console.log(`[setup] ${VU_COUNT}명 준비 시작...`);
  const results = [];
  const ts = Date.now();

  for (let i = 0; i < VU_COUNT; i++) {
    const email    = `kafka-test-${ts}-${i}@test.com`;
    const password = 'Test1234!!';

    // 1. 회원가입
    const signupRes = http.post(
      `${BASE_URL}/api/users/signup`,
      JSON.stringify({ email, password, name: `KafkaUser${i}`, nickName: `kafkauser${i}` }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    if (signupRes.status !== 201) {
      console.warn(`[setup] signup 실패 i=${i} status=${signupRes.status} body=${signupRes.body}`);
      continue;
    }
    const userId = signupRes.json('userId');

    // 2. 로그인
    const loginRes = http.post(
      `${BASE_URL}/api/users/login`,
      JSON.stringify({ email, password }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    const token = loginRes.json('accessToken');
    if (!token) {
      console.warn(`[setup] login 실패 i=${i}`);
      continue;
    }

    const authHeader = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` };

    // 3. 포인트 충전 (좌석 가격 이상으로 충전, 넉넉히 300,000원)
    const chargeRes = http.post(
      `${BASE_URL}/api/points`,
      JSON.stringify({ updatePointAmount: 300000 }),
      { headers: authHeader },
    );
    if (chargeRes.status !== 200) {
      console.warn(`[setup] 포인트 충전 실패 i=${i} status=${chargeRes.status} body=${chargeRes.body}`);
    }

    // 4. 좌석 선점 (i번째 VU가 고유 좌석 선점)
    // 좌석 ID는 환경변수나 DB 조회로 주입 가능; 여기서는 기본 리스트 사용
    const seatIds = [
      4049, 4775, 6899, 14625, 15058,
      25876, 28401, 33817, 36371, 47246,
      47814, 51327, 52821, 53394, 55318,
      59336, 70244, 79115, 82757, 87171,
      // 추가 좌석이 필요하면 DB에서 조회 후 배열 확장
    ];
    const seatId = seatIds[i % seatIds.length];

    const reserveRes = http.post(
      `${BASE_URL}/api/reservations`,
      JSON.stringify({ performanceSeatId: seatId }),
      { headers: authHeader },
    );
    if (reserveRes.status !== 201) {
      console.warn(`[setup] 좌석 선점 실패 i=${i} seatId=${seatId} status=${reserveRes.status} body=${reserveRes.body}`);
      continue;
    }
    const reservationId = reserveRes.json('reservationId');

    results.push({ token, userId, reservationId, seatId });
    sleep(0.05); // BCrypt 부담 분산
  }

  console.log(`[setup] 완료. 결제 준비 ${results.length}건`);
  return { reservations: results };
}

// ── default: 각 VU가 자신의 예약을 결제 확정 ─────────
export default function (data) {
  const idx         = (__VU - 1) % data.reservations.length;
  const { token, reservationId } = data.reservations[idx];

  const res = http.post(
    `${BASE_URL}/api/reservations/${reservationId}/confirm`,
    null,
    {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type':  'application/json',
      },
      timeout: '15s',
      tags:    { name: 'confirm' },
    },
  );

  const ok = check(res, {
    '결제 확정 200': (r) => r.status === 200,
  });

  if (res.status === 200) {
    confirmSuccess.add(1);
    kafkaExpected.add(1);   // 성공 1건 == Kafka 메시지 1개 기대
  } else {
    confirmFailed.add(1);
    console.error(`[VU${__VU}] 결제 확정 실패 reservationId=${reservationId} status=${res.status} body=${res.body}`);
  }
}

// ── handleSummary: 결과 출력 ──────────────────────────
export function handleSummary(data) {
  const dur     = data.metrics['http_req_duration'];
  const tagDur  = data.metrics['http_req_duration{name:confirm}'];
  const reqs    = data.metrics['http_reqs'];
  const succ    = data.metrics['confirm_success'];
  const failed  = data.metrics['confirm_failed'];
  const kafka   = data.metrics['kafka_expected'];

  const ms  = (m, k) => (m && m.values[k] != null ? m.values[k].toFixed(1) : 'N/A');
  const cnt = (m)    => (m ? m.values['count'] : 0);

  const successCount = cnt(succ);
  const failCount    = cnt(failed);
  const kafkaCount   = cnt(kafka);
  const duration_s   = (data.state.testRunDurationMs / 1000).toFixed(1);
  const tps          = successCount > 0 ? (successCount / duration_s).toFixed(2) : 'N/A';

  console.log('\n========== Kafka 결제 확정 부하 테스트 결과 ==========');
  console.log(`[환경]`);
  console.log(`  동시 VU 수          : ${VU_COUNT}`);
  console.log(`  총 요청 수          : ${cnt(reqs)}`);
  console.log(`  테스트 소요 시간    : ${duration_s}s`);
  console.log('');
  console.log(`[결제 확정 API — /api/reservations/{id}/confirm]`);
  console.log(`  성공(200)           : ${successCount}건`);
  console.log(`  실패                : ${failCount}건`);
  console.log(`  처리량(TPS)         : ${tps} req/s`);
  console.log(`  p50 응답시간        : ${ms(tagDur, 'p(50)')} ms`);
  console.log(`  p95 응답시간        : ${ms(tagDur, 'p(95)')} ms`);
  console.log(`  p99 응답시간        : ${ms(tagDur, 'p(99)')} ms`);
  console.log(`  avg 응답시간        : ${ms(tagDur, 'avg')} ms`);
  console.log(`  max 응답시간        : ${ms(tagDur, 'max')} ms`);
  console.log('');
  console.log(`[Kafka 메시지]`);
  console.log(`  예상 발행 수        : ${kafkaCount}건  (성공 요청 수와 동일해야 함)`);
  console.log(`  실측 수신 수        : 앱 로그에서 "[Kafka 수신]" 로그 수 확인`);
  console.log(`  멱등성 중복 차단    : 앱 로그에서 "[Kafka 중복 수신 무시]" 수 확인`);
  console.log('');
  console.log(`[검증 포인트]`);
  console.log(`  ① 발행 수 == 수신 수 → 메시지 손실 없음`);
  console.log(`  ② API p95 < 2000ms  → 비동기 발행이 응답 시간에 영향 없음`);
  console.log(`  ③ 에러율 < 1%       → 분산 환경에서 결제 안정성 확인`);
  console.log('======================================================\n');

  return {
    '/scripts/summary-kafka-payment.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
