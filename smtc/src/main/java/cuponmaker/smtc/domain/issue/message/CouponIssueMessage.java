package cuponmaker.smtc.domain.issue.message;

import java.time.LocalDateTime;

public record CouponIssueMessage(
        Long eventId,
        Long userId,
        LocalDateTime requestedAt
) {
}
