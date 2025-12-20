/**
 * Load Test - Transaction Authorizer
 *
 * Teste de carga para validar performance e confiabilidade
 * do endpoint de criação de transações.
 *
 * @author msvidal
 * @date 2025-12-20
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// Configuração do teste
export const options = {
    stages:  [
        { duration: '30s', target: 50 },  // Ramp-up:  0 -> 50 VUs
        { duration: '1m', target: 50 },   // Sustain: 50 VUs
        { duration:  '30s', target: 0 },   // Ramp-down: 50 -> 0 VUs
    ],

    thresholds: {
        'http_req_duration': ['p(95)<500'],
        'http_req_failed': ['rate<0.01'],
    },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    const transactionId = uuidv4();
    const accountId = uuidv4();

    const url = `${BASE_URL}/transactions/${transactionId}`;
    const payload = JSON.stringify({
        accountId:  accountId,
        amount: {
            value: 10,
            currency: 'BRL'
        },
        operation: 'CREDIT'
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'accept': '*/*',
        },
    };

    const res = http.post(url, payload, params);

    check(res, {
        'status é 200 ou 201': (r) => r.status === 200 || r.status === 201,
        'tempo < 500ms': (r) => r.timings.duration < 500,
        'sem erros': (r) => r.status < 400,
    });

    sleep(1);
}