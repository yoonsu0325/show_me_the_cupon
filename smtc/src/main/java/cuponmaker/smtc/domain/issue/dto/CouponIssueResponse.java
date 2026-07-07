package cuponmaker.smtc.domain.issue.dto;

import cuponmaker.smtc.domain.issue.entity.CouponIssue;
import cuponmaker.smtc.domain.issue.entity.CouponIssueStatus;

import java.time.LocalDateTime;

public record CouponIssueResponse(
        Long issueId,
        Long eventId,
        Long userId,
        CouponIssueStatus status,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt
) {
    public static CouponIssueResponse from(CouponIssue issue) {
        return new CouponIssueResponse(
                issue.getId(),
                issue.getCouponEventId(),
                issue.getUserId(),
                issue.getStatus(),
                issue.getIssuedAt(),
                issue.getExpiresAt()
        );
    }
}
