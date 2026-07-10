package cuponmaker.smtc.domain.issue.message;

import java.time.LocalDateTime;

public record CouponIssueInfo(
            Long eventId,
            LocalDateTime startAt,
            LocalDateTime endAt
) {
}
