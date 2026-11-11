package cuponmaker.smtc.domain.issue.service;

import cuponmaker.smtc.domain.coupon.entity.CouponEvent;
import cuponmaker.smtc.domain.coupon.repository.CouponEventRepository;
import cuponmaker.smtc.domain.issue.dto.CouponIssueAcceptedResponse;
import cuponmaker.smtc.domain.issue.entity.CouponIssue;
import cuponmaker.smtc.domain.issue.message.CouponIssueInfo;
import cuponmaker.smtc.domain.issue.message.CouponIssueMessage;
import cuponmaker.smtc.domain.issue.message.CouponIssueNoLockProducer;
import cuponmaker.smtc.domain.issue.repository.CouponIssueRepository;
import cuponmaker.smtc.global.exception.BaseException;
import cuponmaker.smtc.global.response.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * Redis + RabbitMQ 비동기 발급의 "락 없는" 버전.
 *
 * 요청 단계(Redis 재고 선점 → 메시지 발행)는 기존 issueAsync 와 동일하고,
 * consumer 에서 DB 에 접근할 때 비관적 락(SELECT ... FOR UPDATE) 없이 일반 조회(findById)를 사용한다.
 *
 * 수량 정합성은 Redis 선점 단계에서 보장된다는 전제이며,
 * consumer 가 여러 개(concurrency > 1)로 동시에 같은 이벤트를 처리하면
 * issuedQuantity 갱신이 유실(lost update)될 수 있다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CouponIssueNoLockService {

    private final CouponEventRepository couponEventRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final CouponIssueRedisService couponIssueRedisService;
    private final CouponIssueNoLockProducer couponIssueNoLockProducer;
    private final CouponInfoIssueService couponInfoIssueService;

    public CouponIssueAcceptedResponse issueAsyncNoLock(Long eventId, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        CouponIssueInfo issueInfo = couponInfoIssueService.getIssueInfo(eventId);

        validateIssuePeriod(issueInfo, now);

        couponIssueRedisService.reserve(eventId, userId);

        try {
            couponIssueNoLockProducer.send(new CouponIssueMessage(eventId, userId, now));

            return CouponIssueAcceptedResponse.of(eventId);

        } catch (Exception e) {
            couponIssueRedisService.cancelReservation(eventId, userId);
            throw e;
        }
    }

    @Transactional
    public void processIssueMessage(CouponIssueMessage message) {
        LocalDateTime requestedAt = message.requestedAt();

        try {
            // 락 없이 일반 조회
            CouponEvent couponEvent = couponEventRepository.findById(message.eventId())
                    .orElseThrow(() -> new BaseException(BaseResponseStatus.COUPON_EVENT_NOT_FOUND));

            if (couponIssueRepository.existsByCouponEventIdAndUserId(
                    message.eventId(),
                    message.userId()
            )) {
                return;
            }

            couponEvent.validateIssuable(requestedAt);

            CouponIssue couponIssue = new CouponIssue(
                    message.eventId(),
                    message.userId(),
                    requestedAt,
                    requestedAt.plusDays(couponEvent.getValidDays())
            );

            couponIssueRepository.save(couponIssue);

            couponEvent.increaseIssuedQuantity();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    couponIssueRedisService.confirmIssue(message.eventId());
                }
            });

        } catch (DataIntegrityViolationException e) {
            // 동시에 들어온 중복 메시지가 unique constraint 에 걸린 경우: 이미 발급된 상태이므로 스킵
            log.warn("[no-lock] 동시 중복 발급으로 처리 스킵. eventId={}, userId={}",
                    message.eventId(), message.userId());

        } catch (Exception e) {
            log.error("[no-lock] 쿠폰 발급 메시지 처리 실패. eventId={}, userId={}",
                    message.eventId(), message.userId(), e);
            throw e;
        }
    }

    private void validateIssuePeriod(CouponIssueInfo info, LocalDateTime now) {
        if (now.isBefore(info.startAt())) {
            throw new BaseException(BaseResponseStatus.COUPON_EVENT_NOT_OPEN);
        }

        if (now.isAfter(info.endAt())) {
            throw new BaseException(BaseResponseStatus.COUPON_EVENT_CLOSED);
        }
    }
}
