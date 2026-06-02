/**
 * Wave 5: Kafka 컨슈머 성능 기준선 측정
 *
 * 목적:
 *   대량 메시지 발행 후 컨슈머 그룹(concert-reservation-group)의 lag 감소 속도 측정
 *   ① 컨슈머 처리 TPS (메시지/초)
 *   ② lag = 발행 오프셋 - 커밋 오프셋 (Kafka UI 또는 CLI로 확인)
 *   ③ 기준선: partition=1, concurrency=1, fetch.max.wait.ms=500ms
 *
 * 실행:
 *   # 1단계: 대량 메시지 발행 (이 스크립트)
 *   docker run --rm --network host -v $(pwd)/test/k6-scripts:/scripts \
 *     -e VU_COUNT=100 grafana/k6 run /scripts/kafka-consumer-lag-test.js
 *
 *   # 2단계: 컨슈머 랙 실시간 모니터링
 *   watch -n 1 "docker exec concert-reservation-kafka \
 *     /opt/kafka/bin/kafka-consumer-groups.sh \
 *     --bootstrap-server localhost:9092 \
 *     --describe --group concert-reservation-group"
 *
 *   # Kafka UI에서도 확인 가능: localhost:9000 → consumer groups
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const VU_COUNT = parseInt(__ENV.VU_COUNT || '100');

const confirmSuccess = new Counter('consumer_test_success');
const confirmFailed  = new Counter('consumer_test_failed');

export const options = {
  setupTimeout: '15m',
  scenarios: {
    consumer_lag_test: {
      executor:    'shared-iterations',
      vus:         VU_COUNT,
      iterations:  VU_COUNT,
      maxDuration: '10m',
    },
  },
  thresholds: {
    'http_req_duration{name:confirm}': ['p(95)<5000'],
    http_req_failed: ['rate<0.1'],
  },
};

// 100VU를 위한 신선한 AVAILABLE 좌석 ID (2026-06-02 Wave 6 — Wave 4와 겹치지 않는 ID)
const AVAILABLE_SEAT_IDS = [
  552635, 553038, 562113, 578382, 588543, 591587, 606844, 621761, 625175, 638019,
  643122, 648991, 652661, 657040, 681449, 687532, 691248, 692243, 704282, 705652,
  724873, 740465, 742294, 747089, 751286, 753897, 757375, 762234, 772598, 784292,
  784344, 795608, 806119, 828885, 845723, 858650, 861543, 866064, 867744, 868480,
  869333, 886803, 890074, 895901, 897685, 908567, 909916, 916919, 924742, 932394,
  937428, 939847, 949981, 966871, 976379, 978165, 984367, 987060, 990196, 1002718,
  1002719, 1002720, 1002724, 1002725, 1002726, 1002728, 1002729, 1002730, 1002731, 1002732,
  1002733, 1002735, 1002736, 1002737, 1002738, 1002739, 1002740, 1002743, 1002745, 1002746,
  1002747, 1002749, 1002750, 1002751, 1002753, 1002754, 1002756, 1002757, 1002758, 1002759,
  1002761, 1002762, 1002763, 1002764, 1002768, 1002769, 1002770, 1002771, 1002772, 1002773,
];

export function setup() {
  console.log(`[setup] 컨슈머 랙 테스트: ${VU_COUNT}명 준비...`);
  const results = [];
  const ts = Date.now();

  for (let i = 0; i < VU_COUNT; i++) {
    const email    = `kafka-con-${ts}-${i}@test.com`;
    const password = 'Test1234!!';

    const signupRes = http.post(
      `${BASE_URL}/api/users/signup`,
      JSON.stringify({ email, password, name: `ConUser${i}`, nickName: `conuser${i}` }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    if (signupRes.status !== 201) continue;

    const loginRes = http.post(
      `${BASE_URL}/api/users/login`,
      JSON.stringify({ email, password }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    const token = loginRes.json('accessToken');
    if (!token) continue;

    const authHeader = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` };

    http.post(`${BASE_URL}/api/points`, JSON.stringify({ updatePointAmount: 300000 }), { headers: authHeader });

    const seatId = AVAILABLE_SEAT_IDS[i % AVAILABLE_SEAT_IDS.length];
    const reserveRes = http.post(
      `${BASE_URL}/api/reservations`,
      JSON.stringify({ performanceSeatId: seatId }),
      { headers: authHeader },
    );
    if (reserveRes.status !== 201) continue;

    results.push({ token, reservationId: reserveRes.json('reservationId') });
    sleep(0.05);
  }

  console.log(`[setup] 완료: ${results.length}건`);
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
      timeout: '30s',
      tags:    { name: 'confirm' },
    },
  );

  check(res, { '결제 확정 200': (r) => r.status === 200 });

  if (res.status === 200) confirmSuccess.add(1);
  else confirmFailed.add(1);
}

export function handleSummary(data) {
  const tagDur = data.metrics['http_req_duration{name:confirm}'];
  const reqs   = data.metrics['http_reqs'];
  const succ   = data.metrics['consumer_test_success'];
  const failed = data.metrics['consumer_test_failed'];

  const ms  = (m, k) => (m && m.values[k] != null ? m.values[k].toFixed(1) : 'N/A');
  const cnt = (m)    => (m ? m.values['count'] : 0);

  const successCount = cnt(succ);
  const duration_s   = (data.state.testRunDurationMs / 1000).toFixed(1);

  console.log('\n========== Kafka 컨슈머 랙 테스트 결과 ==========');
  console.log(`  성공(200)           : ${successCount}건  → Kafka 발행 ${successCount}건 기대`);
  console.log(`  실패                : ${cnt(failed)}건`);
  console.log(`  소요 시간           : ${duration_s}s`);
  console.log(`  confirm avg         : ${ms(tagDur, 'avg')} ms`);
  console.log(`  confirm p95         : ${ms(tagDur, 'p(95)')} ms`);
  console.log('');
  console.log('[다음 단계] 컨슈머 랙 확인:');
  console.log('  docker exec concert-reservation-kafka \\');
  console.log('    /opt/kafka/bin/kafka-consumer-groups.sh \\');
  console.log('    --bootstrap-server localhost:9092 \\');
  console.log('    --describe --group concert-reservation-group');
  console.log('  또는 Kafka UI: localhost:9000 → Consumer Groups');
  console.log('=================================================\n');

  return {
    '/scripts/summary-kafka-consumer-baseline.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
