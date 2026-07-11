package cuponmaker.smtc.domain.issue.repository;

import cuponmaker.smtc.domain.issue.entity.CouponIssue;
import cuponmaker.smtc.domain.issue.entity.CouponIssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    boolean existsByCouponEventIdAndUserId(Long couponEventId, Long userId);

    Optional<CouponIssue> findByCouponEventIdAndUserId(Long couponEventId, Long userId);

    List<CouponIssue> findAllByUserIdOrderByIssuedAtDesc(Long userId);

    long countByCouponEventId(Long id);

    long countByCouponEventIdAndUserId(Long id, Long id1);

    long countByStatus(CouponIssueStatus couponIssueStatus);
}
