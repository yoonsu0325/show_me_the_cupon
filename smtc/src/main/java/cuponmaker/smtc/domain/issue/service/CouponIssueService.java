package cuponmaker.smtc.domain.issue.service;

import cuponmaker.smtc.domain.coupon.dto.CouponEventIssueInfo;
import cuponmaker.smtc.domain.coupon.entity.CouponEvent;
import cuponmaker.smtc.domain.coupon.repository.CouponEventRepository;
import cuponmaker.smtc.domain.issue.dto.CouponIssueAcceptedResponse;
import cuponmaker.smtc.domain.issue.dto.CouponIssueResponse;
import cuponmaker.smtc.domain.issue.entity.CouponIssue;
import cuponmaker.smtc.domain.issue.message.CouponIssueInfo;
import cuponmaker.smtc.domain.issue.message.CouponIssueMessage;
import cuponmaker.smtc.domain.issue.message.CouponIssueProducer;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CouponIssueService {

    private final CouponEventRepository couponEventRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final CouponIssueRedisService couponIssueRedisService;
    private final CouponIssueProducer couponIssueProducer;
    private final CouponInfoIssueService couponInfoIssueService;

    @Transactional
    public CouponIssueResponse issue(Long eventId, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        CouponEvent couponEvent = couponEventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COUPON_EVENT_NOT_FOUND));

        if (couponIssueRepository.existsByCouponEventIdAndUserId(eventId, userId)) {
            throw new BaseException(BaseResponseStatus.COUPON_ALREADY_ISSUED);
        }

        couponEvent.issue(now);

        CouponIssue couponIssue = new CouponIssue(
                couponEvent.getId(),
                userId,
                now,
                now.plusDays(couponEvent.getValidDays())
        );

        CouponIssue savedIssue = couponIssueRepository.save(couponIssue);

        return CouponIssueResponse.from(savedIssue);
    }

    @Transactional
    public CouponIssueResponse issueWithRedis(Long eventId, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        CouponEventIssueInfo info = couponEventRepository.findIssueInfoById(eventId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COUPON_EVENT_NOT_FOUND));

        validateIssuePeriod(info, now);

        couponIssueRedisService.reserve(
                eventId,
                userId,
                info.totalQuantity()
        );

        try {
            CouponEvent lockedEvent = couponEventRepository.findByIdForUpdate(eventId)
                    .orElseThrow(() -> new BaseException(BaseResponseStatus.COUPON_EVENT_NOT_FOUND));

            if (couponIssueRepository.existsByCouponEventIdAndUserId(eventId, userId)) {
                throw new BaseException(BaseResponseStatus.COUPON_ALREADY_ISSUED);
            }

            lockedEvent.validateIssuable(now);

            CouponIssue couponIssue = new CouponIssue(
                    eventId,
                    userId,
                    now,
                    now.plusDays(lockedEvent.getValidDays())
            );

            CouponIssue savedIssue = couponIssueRepository.save(couponIssue);

            lockedEvent.increaseIssuedQuantity();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    couponIssueRedisService.confirmIssue(eventId);
                }
            });

            return CouponIssueResponse.from(savedIssue);

        } catch (Exception e) {
            couponIssueRedisService.cancelReservation(eventId, userId);
            throw e;
        }
    }

    @Transactional
    public CouponIssueAcceptedResponse issueAsync(Long eventId, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        CouponIssueInfo issueInfo =
                couponInfoIssueService.getIssueInfo(eventId);

        validateIssuePeriod(issueInfo, now);

        couponIssueRedisService.reserve(
                eventId,
                userId
        );

        try {
            couponIssueProducer.send(
                    new CouponIssueMessage(
                            eventId,
                            userId,
                            now
                    )
            );

            return CouponIssueAcceptedResponse.of(eventId);

        } catch (Exception e) {
            couponIssueRedisService.cancelReservation(eventId, userId);
            throw e;
        }
    }

    public List<CouponIssueResponse> findMyIssues(Long userId) {
        return couponIssueRepository.findAllByUserIdOrderByIssuedAtDesc(userId)
                .stream()
                .map(CouponIssueResponse::from)
                .toList();
    }

    public CouponIssueResponse findMyIssueByEvent(Long eventId, Long userId) {
        CouponIssue issue = couponIssueRepository.findByCouponEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COUPON_ISSUE_NOT_FOUND));

        return CouponIssueResponse.from(issue);
    }

    private void validateIssuePeriod(CouponEventIssueInfo info, LocalDateTime now) {
        if (now.isBefore(info.startAt())) {
            throw new BaseException(BaseResponseStatus.COUPON_EVENT_NOT_OPEN);
        }

        if (now.isAfter(info.endAt())) {
            throw new BaseException(BaseResponseStatus.COUPON_EVENT_CLOSED);
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

    @Transactional
    public void processIssueMessage(CouponIssueMessage message) {
        LocalDateTime requestedAt = message.requestedAt();

        try {
            CouponEvent lockedEvent = couponEventRepository.findByIdForUpdate(message.eventId())
                    .orElseThrow(() -> new BaseException(BaseResponseStatus.COUPON_EVENT_NOT_FOUND));

            if (couponIssueRepository.existsByCouponEventIdAndUserId(
                    message.eventId(),
                    message.userId()
            )) {
                return;
            }

            lockedEvent.validateIssuable(message.requestedAt());

            CouponIssue couponIssue = new CouponIssue(
                    message.eventId(),
                    message.userId(),
                    requestedAt,
                    requestedAt.plusDays(lockedEvent.getValidDays())
            );

            couponIssueRepository.save(couponIssue);

            lockedEvent.increaseIssuedQuantity();

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    couponIssueRedisService.confirmIssue(message.eventId());
                }
            });

        } catch (DataIntegrityViolationException e) {
            // 동시에 들어온 중복 메시지가 unique constraint에 걸린 경우.
            // 이미 정상 발급된 상태이므로 DLQ로 보내지 않고 여기서 종료.
            log.warn("동시 중복 발급으로 처리 스킵. eventId={}, userId={}",
                    message.eventId(), message.userId());

        } catch (Exception e) {
            // 진짜 실패만 재던져서 DLQ로 라우팅
            log.error("쿠폰 발급 메시지 처리 실패, DLQ로 전달. eventId={}, userId={}",
                    message.eventId(), message.userId(), e);
            throw e;
        }

    }
}
