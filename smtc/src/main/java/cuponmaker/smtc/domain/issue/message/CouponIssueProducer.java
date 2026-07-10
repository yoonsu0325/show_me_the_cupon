package cuponmaker.smtc.domain.issue.message;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static cuponmaker.smtc.global.rabbitmq.RabbitMqProperties.COUPON_ISSUE_EXCHANGE;
import static cuponmaker.smtc.global.rabbitmq.RabbitMqProperties.COUPON_ISSUE_ROUTING_KEY;

@Component
@RequiredArgsConstructor
public class CouponIssueProducer {

    private final RabbitTemplate rabbitTemplate;

    public void send(CouponIssueMessage message) {
        rabbitTemplate.convertAndSend(
                COUPON_ISSUE_EXCHANGE,
                COUPON_ISSUE_ROUTING_KEY,
                message
        );
    }
}
