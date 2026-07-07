package cuponmaker.smtc.domain.issue.repository;

import cuponmaker.smtc.domain.issue.entity.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    boolean existsByCouponEventIdAndUserId(Long couponEventId, Long userId);

    Optional<CouponIssue> findByCouponEventIdAndUserId(Long couponEventId, Long userId);

    List<CouponIssue> findAllByUserIdOrderByIssuedAtDesc(Long userId);
}
