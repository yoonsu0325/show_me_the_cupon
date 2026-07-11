package cuponmaker.smtc.domain.issue.message;

import cuponmaker.smtc.domain.issue.service.CouponIssueRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static cuponmaker.smtc.global.rabbitmq.RabbitMqProperties.COUPON_ISSUE_DLQ;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponIssueDlqConsumer {

    private final CouponIssueRedisService couponIssueRedisService;

    @RabbitListener(queues = COUPON_ISSUE_DLQ)
    public void consumeDlq(CouponIssueMessage message) {
        log.error(
                "쿠폰 발급 최종 실패 DLQ 처리. eventId={}, userId={}",
                message.eventId(),
                message.userId()
        );

        couponIssueRedisService.cancelReservation(
                message.eventId(),
                message.userId()
        );
    }
}
