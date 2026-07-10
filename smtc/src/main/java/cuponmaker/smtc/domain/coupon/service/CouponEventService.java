package cuponmaker.smtc.domain.coupon.service;


import cuponmaker.smtc.domain.coupon.dto.CouponEventCreateRequest;
import cuponmaker.smtc.domain.coupon.dto.CouponEventResponse;
import cuponmaker.smtc.domain.coupon.entity.CouponEvent;
import cuponmaker.smtc.domain.coupon.repository.CouponEventRepository;
import cuponmaker.smtc.domain.issue.service.CouponInfoIssueService;
import cuponmaker.smtc.domain.issue.service.CouponIssueRedisService;
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
public class CouponEventService {

    private final CouponEventRepository couponEventRepository;
    private final CouponInfoIssueService couponInfoIssueService;
    private final CouponIssueRedisService couponIssueRedisService;

    @Transactional
    public CouponEventResponse create(CouponEventCreateRequest request) {
        CouponEvent couponEvent = new CouponEvent(
                request.title(),
                request.description(),
                request.totalQuantity(),
                request.validDays(),
                request.startAt(),
                request.endAt()
        );

        CouponEvent savedEvent = couponEventRepository.save(couponEvent);

        couponInfoIssueService.saveIssueInfo(
                savedEvent.getId(),
                savedEvent.getStartAt(),
                savedEvent.getEndAt()
        );

        couponIssueRedisService.initializeStock(
                savedEvent.getId(),
                savedEvent.getTotalQuantity(),
                savedEvent.getEndAt()
        );

        return CouponEventResponse.from(savedEvent);
    }

    public List<CouponEventResponse> findAll() {
        return couponEventRepository.findAll()
                .stream()
                .map(CouponEventResponse::from)
                .toList();
    }

    public CouponEventResponse findById(Long eventId) {
        CouponEvent event = couponEventRepository.findById(eventId)
                .orElseThrow(() -> new BaseException(BaseResponseStatus.COUPON_EVENT_NOT_FOUND));

        return CouponEventResponse.from(event);
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new BaseException(BaseResponseStatus.INVALID_COUPON_EVENT_PERIOD);
        }
    }

}
