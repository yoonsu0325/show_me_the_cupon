package cuponmaker.smtc.domain.issue.message;

import cuponmaker.smtc.domain.issue.service.CouponIssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static cuponmaker.smtc.global.rabbitmq.RabbitMqProperties.COUPON_ISSUE_QUEUE;

@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final CouponIssueService couponIssueService;

    @RabbitListener(queues = COUPON_ISSUE_QUEUE)
    public void consume(CouponIssueMessage message) {
        couponIssueService.processIssueMessage(message);
    }
}
