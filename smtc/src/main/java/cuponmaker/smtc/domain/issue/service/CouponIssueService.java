package cuponmaker.smtc.domain.issue.service;

import cuponmaker.smtc.domain.coupon.entity.CouponEvent;
import cuponmaker.smtc.domain.coupon.repository.CouponEventRepository;
import cuponmaker.smtc.domain.issue.dto.CouponIssueResponse;
import cuponmaker.smtc.domain.issue.entity.CouponIssue;
import cuponmaker.smtc.domain.issue.repository.CouponIssueRepository;
import cuponmaker.smtc.global.exception.BaseException;
import cuponmaker.smtc.global.response.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponIssueService {

    private final CouponEventRepository couponEventRepository;
    private final CouponIssueRepository couponIssueRepository;

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
}
