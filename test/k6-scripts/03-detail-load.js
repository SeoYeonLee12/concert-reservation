import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

export const options = {
  stages: [
    { duration: '10s', target: 100 },
    { duration: '40s', target: 100 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed:   ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    checks:            ['rate>0.99'],
  },
};

export default function () {
  const id = Math.floor(Math.random() * 1000) + 1;
  const res = http.get(
    `${BASE_URL}/api/performances/${id}`,
    {
      tags: { scenario: 'detail-load' },
    },
  );
  check(res, {
    '[detail-load] status is 200 or 404': (r) => r.status === 200 || r.status === 404,
  });
  sleep(0.1);
}

export function handleSummary(data) {
  return {
    '/scripts/summary-detail-load.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
