package cuponmaker.smtc.domain.issue.message;

import cuponmaker.smtc.domain.issue.repository.CouponIssueRepository;
import cuponmaker.smtc.domain.issue.service.CouponIssueRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static cuponmaker.smtc.global.rabbitmq.RabbitMqProperties.COUPON_ISSUE_DLQ;
import static cuponmaker.smtc.global.rabbitmq.RabbitMqProperties.DLQ_LISTENER_CONTAINER_FACTORY;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponIssueDlqConsumer {

    private final CouponIssueRedisService couponIssueRedisService;
    private final CouponIssueRepository couponIssueRepository;

    @RabbitListener(queues = COUPON_ISSUE_DLQ, containerFactory = DLQ_LISTENER_CONTAINER_FACTORY)
    public void consumeDlq(
            CouponIssueMessage message,
            @Header(name = RepublishMessageRecoverer.X_EXCEPTION_MESSAGE, required = false) String exceptionMessage,
            @Header(name = RepublishMessageRecoverer.X_ORIGINAL_ROUTING_KEY, required = false) String originalRoutingKey
    ) {
        log.error(
                "쿠폰 발급 최종 실패 DLQ 처리. eventId={}, userId={}, originalRoutingKey={}, cause={}",
                message.eventId(),
                message.userId(),
                originalRoutingKey,
                exceptionMessage
        );

        if (couponIssueRepository.existsByCouponEventIdAndUserId(message.eventId(), message.userId())) {
            return; // 이미 발급됨 → 재고 복구하지 않음
        }

        try {
            couponIssueRedisService.cancelReservation(
                    message.eventId(),
                    message.userId()
            );
        } catch (Exception e) {
            // DLQ 처리까지 실패한 경우: 재시도/재발행 없이 폐기되므로 수동 복구를 위해 상세 로그를 남긴다.
            log.error("DLQ 보상 처리(예약 취소) 실패. 수동 확인 필요. eventId={}, userId={}",
                    message.eventId(), message.userId(), e);
            throw e;
        }
    }
}
