import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://host.docker.internal:8080';

export const options = {
  vus: 50,
  duration: '30s',
  thresholds: {
    http_req_failed:   ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    checks:            ['rate>0.99'],
  },
};

export default function () {
  const res = http.get(`${BASE}/api/performances`, {
    tags: { endpoint: 'list' },
  });
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(0.1);
}
