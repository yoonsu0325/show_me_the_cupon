package cuponmaker.smtc.domain.coupon.repository;

import cuponmaker.smtc.domain.coupon.entity.CouponEvent;
import cuponmaker.smtc.domain.coupon.dto.CouponEventIssueInfo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponEventRepository extends JpaRepository<CouponEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CouponEvent c where c.id = :eventId")
    Optional<CouponEvent> findByIdForUpdate(@Param("eventId") Long eventId);

    @Query("""
    select new cuponmaker.smtc.domain.coupon.dto.CouponEventIssueInfo(
        c.id,
        c.totalQuantity,
        c.validDays,
        c.startAt,
        c.endAt
    )
    from CouponEvent c
    where c.id = :eventId
""")
    Optional<CouponEventIssueInfo> findIssueInfoById(@Param("eventId") Long eventId);
}
