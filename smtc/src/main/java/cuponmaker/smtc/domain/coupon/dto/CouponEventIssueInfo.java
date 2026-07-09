package cuponmaker.smtc.domain.coupon.dto;

import java.time.LocalDateTime;

public record CouponEventIssueInfo(
        Long eventId,
        int totalQuantity,
        int validDays,
        LocalDateTime startAt,
        LocalDateTime endAt

) {

}
