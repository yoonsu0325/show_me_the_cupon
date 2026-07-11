package cuponmaker.smtc.domain.issue.entity;

import cuponmaker.smtc.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "coupon_issues",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_coupon_issues_event_user",
                        columnNames = {"coupon_event_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_coupon_issues_user_id", columnList = "user_id"),
                @Index(name = "idx_coupon_issues_event_id", columnList = "coupon_event_id"),
                @Index(name = "idx_coupon_issues_status_expires_at", columnList = "status, expires_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_event_id", nullable = false)
    private Long couponEventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponIssueStatus status;

    @Column(nullable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

    public CouponIssue(
            Long couponEventId,
            Long userId,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt
    ) {
        this.couponEventId = couponEventId;
        this.userId = userId;
        this.status = CouponIssueStatus.ISSUED;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public void expire(LocalDateTime now) {
        if (this.status == CouponIssueStatus.ISSUED && this.expiresAt.isBefore(now)) {
            this.status = CouponIssueStatus.EXPIRED;
        }
    }

    public void changeStatusForTest(CouponIssueStatus status) {
        this.status = status;
    }
}
