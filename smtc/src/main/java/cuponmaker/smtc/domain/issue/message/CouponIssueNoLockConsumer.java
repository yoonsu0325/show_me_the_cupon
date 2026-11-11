package cuponmaker.smtc.domain.issue.message;

import cuponmaker.smtc.domain.issue.service.CouponIssueNoLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static cuponmaker.smtc.global.rabbitmq.RabbitMqNoLockProperties.COUPON_ISSUE_NO_LOCK_QUEUE;

/**
 * 기본 rabbitListenerContainerFactory 를 사용하므로
 * 기존 consumer 와 동일하게 최대 3회 재시도 후 DLQ 로 전달된다.
 */
@Component
@RequiredArgsConstructor
public class CouponIssueNoLockConsumer {

    private final CouponIssueNoLockService couponIssueNoLockService;

    @RabbitListener(queues = COUPON_ISSUE_NO_LOCK_QUEUE)
    public void consume(CouponIssueMessage message) {
        couponIssueNoLockService.processIssueMessage(message);
    }
}
