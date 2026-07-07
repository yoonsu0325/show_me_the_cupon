package cuponmaker.smtc.domain.coupon.dto;

import cuponmaker.smtc.domain.coupon.entity.CouponEvent;
import cuponmaker.smtc.domain.coupon.entity.CouponEventStatus;

import java.time.LocalDateTime;

public record CouponEventResponse(
        Long eventId,
        String title,
        String description,
        int totalQuantity,
        int issuedQuantity,
        int remainingQuantity,
        int validDays,
        LocalDateTime startAt,
        LocalDateTime endAt,
        CouponEventStatus status
) {
    public static CouponEventResponse from(CouponEvent event) {
        return new CouponEventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getTotalQuantity(),
                event.getIssuedQuantity(),
                event.getRemainingQuantity(),
                event.getValidDays(),
                event.getStartAt(),
                event.getEndAt(),
                event.getStatus()
        );
    }
}
