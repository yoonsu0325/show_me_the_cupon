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
}
