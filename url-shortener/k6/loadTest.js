import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 10 },
        { duration: '1m',  target: 10 },
        { duration: '30s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<200'],  // relaxed for remote DB
        http_req_failed: ['rate<0.05'],
    },
};

const BASE_URL = 'http://localhost:8080';
const SHORT_CODE = 'e'; // use existing code already in DB and Redis

export default function () {
    // Only test redirect — this hits Redis L1 cache, should be fast
    const redirectRes = http.get(`${BASE_URL}/${SHORT_CODE}`, {
        redirects: 0,
    });

    check(redirectRes, {
        'redirect status 302': (r) => r.status === 302,
    });

    sleep(0.5);
}