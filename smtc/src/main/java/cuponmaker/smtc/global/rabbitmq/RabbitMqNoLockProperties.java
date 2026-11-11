package cuponmaker.smtc.global.rabbitmq;

/**
 * 락 없이 DB 에 접근하는 비동기 쿠폰 발급(no-lock) 버전 전용 RabbitMQ 상수.
 * Exchange / DLX / DLQ 는 기존 비동기 발급과 공유하고, 큐와 라우팅 키만 분리한다.
 */
public final class RabbitMqNoLockProperties {

    private RabbitMqNoLockProperties() {
    }

    public static final String COUPON_ISSUE_NO_LOCK_QUEUE = "coupon.issue.nolock.queue";
    public static final String COUPON_ISSUE_NO_LOCK_ROUTING_KEY = "coupon.issue.nolock";
}
