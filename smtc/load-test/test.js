import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Counter } from 'k6/metrics';

// 커스텀 메트릭 정의
export const businessSuccess = new Counter('business_success');
export const businessFail = new Counter('business_fail');
export const soldOutCount = new Counter('sold_out_count');
export const duplicateCount = new Counter('duplicate_count');

// 환경 변수 설정 (기본값 지정)
const VUS = Number(__ENV.VUS || 500);
const EVENT_ID = __ENV.EVENT_ID || 1;
const TYPE = __ENV.TYPE || 1;

export const options = {
    // 단계별 부하증대(Ramp-up) 시나리오 적용
    stages: [
        { duration: '20s', target: 50 },    // 1단계: 50 VUs로 워밍업
        { duration: '30s', target: 200 },   // 2단계: 200 VUs로 부하증대
        { duration: '30s', target: VUS },   // 3단계: 목표 VUs (기본 500)까지 상승
        { duration: '30s', target: VUS },   // 4단계: 최고 부하 유지
        { duration: '15s', target: 0 },     // 5단계: 쿨다운 (종료)
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],     // HTTP 에러율 5 미만 유지
        http_req_duration: ['p(95)<1000'],  // 95% 요청이 1초 이내 응답
    },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    // 고유한 유저 ID 생성 (iteration 번호 활용)
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

    const isSuccess = check(res, {
        'status is 200': (r) => r.status === 200,
    });

    // 성능 측정을 왜곡하는 console.log 제거 완료

    if (isSuccess && res.body) {
        try {
            const body = JSON.parse(res.body);

            if (body.success === true) {
                businessSuccess.add(1);
            } else {
                businessFail.add(1);

                if (body.code === 'COUPON_SOLD_OUT') {
                    soldOutCount.add(1);
                } else if (body.code === 'COUPON_ALREADY_ISSUED') {
                    duplicateCount.add(1);
                }
            }
        } catch (e) {
            businessFail.add(1);
        }
    } else {
        businessFail.add(1);
    }
}
