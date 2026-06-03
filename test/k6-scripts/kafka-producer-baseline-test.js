/**
 * Wave 3: Kafka 프로듀서 성능 기준선 측정
 *
 * 목적:
 *   결제 확정 API → @Async Kafka 발행 경로의 프로듀서 성능 측정
 *   ① API 응답 시간 (비동기 발행이 API 지연에 영향 없음 검증)
 *   ② Kafka 발행 성공/실패 수 (앱 로그의 [Kafka 발행 완료] 수로 교차 검증)
 *   ③ 기준선 TPS — 개선(lz4 압축) 전후 비교 기준점
 *
 * 실행:
 *   docker run --rm --network host -v $(pwd)/test/k6-scripts:/scripts \
 *     grafana/k6 run /scripts/kafka-producer-baseline-test.js
 *
 * 측정 후 확인:
 *   - Kafka UI (localhost:9000): payment.confirmed 토픽 오프셋 증가 확인
 *   - 앱 로그: docker logs concert-reservation-app 2>&1 | grep "Kafka 발행"
 *   - 컨슈머 렉: docker exec concert-reservation-kafka /opt/kafka/bin/kafka-consumer-groups.sh \
 *       --bootstrap-server localhost:9092 --describe --group concert-reservation-group
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const VU_COUNT = parseInt(__ENV.VU_COUNT || '50');

// ── 커스텀 메트릭 ─────────────────────────────────────────────────
const confirmSuccess  = new Counter('kafka_producer_success');  // 결제 확정 성공 → Kafka 발행 기대
const confirmFailed   = new Counter('kafka_producer_failed');   // 결제 확정 실패
const kafkaExpected   = new Counter('kafka_messages_expected'); // 발행 기대 수

export const options = {
  setupTimeout: '10m',
  scenarios: {
    producer_baseline: {
      executor:    'shared-iterations',
      vus:         VU_COUNT,
      iterations:  VU_COUNT,
      maxDuration: '5m',
    },
  },
  thresholds: {
    'http_req_duration{name:confirm}': ['p(95)<3000'],
    http_req_failed:                   ['rate<0.05'],
  },
};

// ── 신선한 AVAILABLE 좌석 ID (2026-06-02 Wave 4 — ID > 203) ─────────
const AVAILABLE_SEAT_IDS = [
  128248, 136763, 139354, 145038, 149135, 159120, 165656, 175025, 185233, 187126,
  187949, 216384, 221880, 224487, 228156, 239175, 255655, 264334, 271264, 273954,
  278847, 280049, 292215, 293998, 295906, 297343, 297403, 309584, 322959, 347516,
  349002, 349955, 351795, 370977, 377256, 384625, 389047, 397904, 406136, 424845,
  463903, 468646, 470732, 489089, 495793, 510635, 521804, 539807, 542608, 550517,
];

export function setup() {
  console.log(`[setup] 프로듀서 기준선 테스트: ${VU_COUNT}명 준비 시작...`);
  const results = [];
  const ts = Date.now();

  for (let i = 0; i < VU_COUNT; i++) {
    const email    = `kafka-prod-${ts}-${i}@test.com`;
    const password = 'Test1234!!';

    const signupRes = http.post(
      `${BASE_URL}/api/users/signup`,
      JSON.stringify({ email, password, name: `ProdUser${i}`, nickName: `produser${i}` }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    if (signupRes.status !== 201) {
      console.warn(`[setup] signup 실패 i=${i}: ${signupRes.status}`);
      continue;
    }

    const loginRes = http.post(
      `${BASE_URL}/api/users/login`,
      JSON.stringify({ email, password }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    const token = loginRes.json('accessToken');
    if (!token) continue;

    const authHeader = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` };

    http.post(
      `${BASE_URL}/api/points`,
      JSON.stringify({ updatePointAmount: 300000 }),
      { headers: authHeader },
    );

    const seatId = AVAILABLE_SEAT_IDS[i % AVAILABLE_SEAT_IDS.length];
    const reserveRes = http.post(
      `${BASE_URL}/api/reservations`,
      JSON.stringify({ performanceSeatId: seatId }),
      { headers: authHeader },
    );
    if (reserveRes.status !== 201) {
      console.warn(`[setup] 선점 실패 i=${i} seatId=${seatId}: ${reserveRes.status}`);
      continue;
    }

    results.push({ token, reservationId: reserveRes.json('reservationId'), seatId });
    sleep(0.05);
  }

  console.log(`[setup] 완료: ${results.length}건 결제 대기 중`);
  return { reservations: results };
}

export default function (data) {
  if (!data.reservations.length) return;
  const { token, reservationId } = data.reservations[(__VU - 1) % data.reservations.length];

  const res = http.post(
    `${BASE_URL}/api/reservations/${reservationId}/confirm`,
    null,
    {
      headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json' },
      timeout: '15s',
      tags:    { name: 'confirm' },
    },
  );

  check(res, { '결제 확정 200': (r) => r.status === 200 });

  if (res.status === 200) {
    confirmSuccess.add(1);
    kafkaExpected.add(1);
  } else {
    confirmFailed.add(1);
    console.error(`[VU${__VU}] 실패 reservationId=${reservationId} status=${res.status}`);
  }
}

export function handleSummary(data) {
  const tagDur  = data.metrics['http_req_duration{name:confirm}'];
  const reqs    = data.metrics['http_reqs'];
  const succ    = data.metrics['kafka_producer_success'];
  const failed  = data.metrics['kafka_producer_failed'];

  const ms  = (m, k) => (m && m.values[k] != null ? m.values[k].toFixed(1) : 'N/A');
  const cnt = (m)    => (m ? m.values['count'] : 0);

  const successCount = cnt(succ);
  const duration_s   = (data.state.testRunDurationMs / 1000).toFixed(1);
  const tps          = duration_s > 0 ? (cnt(reqs) / duration_s).toFixed(2) : 'N/A';

  console.log('\n========== Kafka 프로듀서 기준선 ==========');
  console.log(`[환경]`);
  console.log(`  VU 수               : ${VU_COUNT}`);
  console.log(`  총 요청 수          : ${cnt(reqs)}`);
  console.log(`  소요 시간           : ${duration_s}s`);
  console.log('');
  console.log(`[결제 확정 API — /confirm]`);
  console.log(`  성공(200)           : ${successCount}건`);
  console.log(`  실패                : ${cnt(failed)}건`);
  console.log(`  TPS                 : ${tps} req/s`);
  console.log(`  avg                 : ${ms(tagDur, 'avg')} ms`);
  console.log(`  p95                 : ${ms(tagDur, 'p(95)')} ms`);
  console.log(`  max                 : ${ms(tagDur, 'max')} ms`);
  console.log('');
  console.log(`[Kafka 발행 기대]`);
  console.log(`  기대 발행 수        : ${successCount}건`);
  console.log(`  실제 확인 방법:`);
  console.log(`    docker logs concert-reservation-app 2>&1 | grep "Kafka 발행 완료" | wc -l`);
  console.log(`    → Kafka UI localhost:9000 에서 payment.confirmed 오프셋 확인`);
  console.log('');
  console.log(`[개선 전 기준선 기록]`);
  console.log(`  compression.type    : none (기본값)`);
  console.log(`  이 수치를 lz4 압축 적용 후와 비교할 것`);
  console.log('==========================================\n');

  return {
    '/scripts/summary-kafka-producer-baseline.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
