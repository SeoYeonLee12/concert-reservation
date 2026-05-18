/**
 * Cache Stampede 부하 테스트
 *
 * 캐시 만료 순간 동시 요청 폭증(Stampede) 발생 시
 * 세 가지 전략의 p95/p99 차이를 측정한다.
 *
 * 전략:
 *   STRATEGY=none        → 보호 없는 @Cacheable (Before)
 *   STRATEGY=sync        → @Cacheable(sync=true) JVM 동기화 (After v1)
 *   STRATEGY=distributed → Redisson 분산 락 (After v2)
 *
 * 실행 방법 (매 실행 전 redis-cli FLUSHDB 필수):
 *   docker exec concert-reservation-redis redis-cli FLUSHDB
 *   docker run --rm -v $(pwd)/test/k6-scripts:/scripts \
 *     -e STRATEGY=none grafana/k6 run /scripts/performance-cache-test.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

const BASE_URL = __ENV.BASE_URL  || 'http://host.docker.internal:8080';
const STRATEGY = __ENV.STRATEGY  || 'sync';

export const options = {
  // 캐시 빈 상태에서 100 VU 동시 접속 → Stampede 순간 포착
  stages: [
    { duration: '5s',  target: 100 }, // 빠른 램프업 — 캐시 빈 상태 유지
    { duration: '50s', target: 100 }, // 유지 — 캐시 채워진 후 안정 구간 비교
    { duration: '5s',  target: 0   }, // 램프다운
  ],
  thresholds: {
    http_req_failed:   ['rate<0.05'],
    http_req_duration: ['p(95)<3000', 'p(99)<5000'],
    checks:            ['rate>0.95'],
  },
};

export default function () {
  const res = http.get(
    `${BASE_URL}/api/performances?strategy=${STRATEGY}`,
    {
      tags: { strategy: STRATEGY, scenario: 'cache-stampede' },
      timeout: '10s',
    },
  );

  check(res, {
    [`[${STRATEGY}] status 200`]: (r) => r.status === 200,
  });

  sleep(0.05); // 최소 sleep — 동시성 최대화
}

export function handleSummary(data) {
  return {
    [`/scripts/summary-cache-stampede-${STRATEGY}.json`]: JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
