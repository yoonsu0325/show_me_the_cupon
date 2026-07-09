package cuponmaker.smtc.domain.coupon.entity;

import cuponmaker.smtc.global.common.BaseTimeEntity;
import cuponmaker.smtc.global.exception.BaseException;
import cuponmaker.smtc.global.response.BaseResponseStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int issuedQuantity;

    @Column(nullable = false)
    private int validDays;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponEventStatus status;

    public CouponEvent(
            String title,
            String description,
            int totalQuantity,
            int validDays,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        this.title = title;
        this.description = description;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.validDays = validDays;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = CouponEventStatus.READY;
    }

    public int getRemainingQuantity() {
        return totalQuantity - issuedQuantity;
    }

    public void issue(LocalDateTime now) {
        validateIssuable(now);
        increaseIssuedQuantity();
    }

    public void validateIssuable(LocalDateTime now) {
        if (now.isBefore(startAt)) {
            throw new BaseException(BaseResponseStatus.COUPON_EVENT_NOT_OPEN);
        }

        if (now.isAfter(endAt)) {
            throw new BaseException(BaseResponseStatus.COUPON_EVENT_CLOSED);
        }

        if (issuedQuantity >= totalQuantity) {
            throw new BaseException(BaseResponseStatus.COUPON_SOLD_OUT);
        }
    }

    public void increaseIssuedQuantity() {
        this.issuedQuantity++;

        if (this.issuedQuantity >= this.totalQuantity) {
            this.status = CouponEventStatus.SOLD_OUT;
        }
    }
}
