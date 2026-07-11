import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';

export const businessSuccess = new Counter('business_success');
export const businessFail = new Counter('business_fail');
export const soldOutCount = new Counter('sold_out_count');
export const duplicateCount = new Counter('duplicate_count');

const VUS = Number(__ENV.VUS || 200);
const ITERATIONS = Number(__ENV.ITERATIONS || 2000);

export const options = {
    scenarios: {
        coupon_issue: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: ITERATIONS,
            maxDuration: '2m',
        },
    },
};

const BASE_URL = 'http://localhost:8080';
const EVENT_ID = __ENV.EVENT_ID;
const TYPE = __ENV.TYPE;

export default function () {
    const userId = exec.scenario.iterationInTest + 1;

    const url = `${BASE_URL}/api/load-test/coupon-events/${EVENT_ID}/issues/${TYPE}`;

    const res = http.post(
        url,
        JSON.stringify({ userId }),
        {
            headers: {
                'Content-Type': 'application/json',
            },
        }
    );

    check(res, {
        'http status is 200': (r) => r.status === 200,
    });

    const body = JSON.parse(res.body);

    if (body.success === true) {
        businessSuccess.add(1);
    } else {
        businessFail.add(1);

        if (body.code === 'COUPON_SOLD_OUT') {
            soldOutCount.add(1);
        }

        if (body.code === 'COUPON_ALREADY_ISSUED') {
            duplicateCount.add(1);
        }
    }
}
