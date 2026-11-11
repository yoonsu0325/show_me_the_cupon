package cuponmaker.smtc.global.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static cuponmaker.smtc.global.rabbitmq.RabbitMqNoLockProperties.COUPON_ISSUE_NO_LOCK_QUEUE;
import static cuponmaker.smtc.global.rabbitmq.RabbitMqNoLockProperties.COUPON_ISSUE_NO_LOCK_ROUTING_KEY;
import static cuponmaker.smtc.global.rabbitmq.RabbitMqProperties.COUPON_ISSUE_DLQ_ROUTING_KEY;
import static cuponmaker.smtc.global.rabbitmq.RabbitMqProperties.COUPON_ISSUE_DLX;

/**
 * no-lock 비동기 발급용 큐 설정.
 * 기존 coupon.issue.exchange 에 별도 라우팅 키로 바인딩하고,
 * 최종 실패 메시지는 기존 DLX → DLQ 로 보내 같은 보상 로직(예약 취소)을 탄다.
 */
@Configuration
public class RabbitMqNoLockConfig {

    @Bean
    public Queue couponIssueNoLockQueue() {
        return QueueBuilder.durable(COUPON_ISSUE_NO_LOCK_QUEUE)
                .withArgument("x-dead-letter-exchange", COUPON_ISSUE_DLX)
                .withArgument("x-dead-letter-routing-key", COUPON_ISSUE_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding couponIssueNoLockBinding(
            @Qualifier("couponIssueNoLockQueue") Queue couponIssueNoLockQueue,
            @Qualifier("couponIssueExchange") DirectExchange couponIssueExchange
    ) {
        return BindingBuilder
                .bind(couponIssueNoLockQueue)
                .to(couponIssueExchange)
                .with(COUPON_ISSUE_NO_LOCK_ROUTING_KEY);
    }
}
