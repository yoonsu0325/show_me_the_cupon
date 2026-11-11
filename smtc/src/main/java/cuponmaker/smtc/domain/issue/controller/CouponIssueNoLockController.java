package cuponmaker.smtc.domain.issue.controller;

import cuponmaker.smtc.domain.issue.dto.CouponIssueAcceptedResponse;
import cuponmaker.smtc.domain.issue.dto.LoadTestIssueRequest;
import cuponmaker.smtc.domain.issue.dto.LoadTestIssueResponse;
import cuponmaker.smtc.domain.issue.service.CouponIssueNoLockService;
import cuponmaker.smtc.global.exception.BaseException;
import cuponmaker.smtc.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Coupon Issue (No Lock)", description = "락 없는 비동기 쿠폰 발급 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CouponIssueNoLockController {

    private final CouponIssueNoLockService couponIssueNoLockService;

    @Operation(summary = "RabbitMQ 기반 비동기 쿠폰 발급 요청 (consumer DB 락 없음)")
    @PostMapping("/coupon-events/{eventId}/issues/async-no-lock")
    public CouponIssueAcceptedResponse issueAsyncNoLock(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return couponIssueNoLockService.issueAsyncNoLock(eventId, userDetails.getUserId());
    }

    @Operation(summary = "[부하 테스트] 비동기 쿠폰 발급 요청 (consumer DB 락 없음)")
    @PostMapping("/load-test/coupon-events/{eventId}/issues/async-no-lock")
    public LoadTestIssueResponse issueAsyncNoLockForLoadTest(
            @PathVariable Long eventId,
            @RequestBody LoadTestIssueRequest request
    ) {
        try {
            couponIssueNoLockService.issueAsyncNoLock(eventId, request.userId());

            return new LoadTestIssueResponse(
                    true,
                    "ACCEPTED",
                    "쿠폰 발급 요청 접수 성공"
            );

        } catch (BaseException e) {
            return new LoadTestIssueResponse(
                    false,
                    String.valueOf(e.getStatus().getCode()),
                    e.getStatus().getMessage()
            );
        }
    }
}
