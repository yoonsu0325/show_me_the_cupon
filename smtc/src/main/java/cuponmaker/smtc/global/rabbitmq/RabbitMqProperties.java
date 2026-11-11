package cuponmaker.smtc.global.rabbitmq;

public final class RabbitMqProperties {

    private RabbitMqProperties() {
    }

    public static final String COUPON_ISSUE_EXCHANGE = "coupon.issue.exchange";
    public static final String COUPON_ISSUE_QUEUE = "coupon.issue.queue";
    public static final String COUPON_ISSUE_ROUTING_KEY = "coupon.issue";

    public static final String COUPON_ISSUE_DLX = "coupon.issue.dlx";
    public static final String COUPON_ISSUE_DLQ = "coupon.issue.dlq";
    public static final String COUPON_ISSUE_DLQ_ROUTING_KEY = "coupon.issue.dlq";

    // 재시도 정책: 최초 1회 + 재시도 최대 3회 = 총 4회 시도
    public static final int COUPON_ISSUE_MAX_RETRY = 3;
    public static final int COUPON_ISSUE_MAX_ATTEMPTS = COUPON_ISSUE_MAX_RETRY + 1;
    public static final long RETRY_INITIAL_INTERVAL_MS = 1000L;
    public static final double RETRY_MULTIPLIER = 2.0;
    public static final long RETRY_MAX_INTERVAL_MS = 5000L;

    public static final String DLQ_LISTENER_CONTAINER_FACTORY = "dlqListenerContainerFactory";
}
