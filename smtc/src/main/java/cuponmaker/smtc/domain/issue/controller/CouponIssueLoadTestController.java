package cuponmaker.smtc.domain.issue.controller;

import cuponmaker.smtc.domain.issue.dto.LoadTestIssueRequest;
import cuponmaker.smtc.domain.issue.dto.LoadTestIssueResponse;
import cuponmaker.smtc.domain.issue.service.CouponIssueService;
import cuponmaker.smtc.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/load-test/coupon-events")
public class CouponIssueLoadTestController {

    private final CouponIssueService couponIssueService;

    @PostMapping("/{eventId}/issues/db-lock")
    public LoadTestIssueResponse issueWithDbLock(
            @PathVariable Long eventId,
            @RequestBody LoadTestIssueRequest request
    ) {
        try {
            couponIssueService.issue(eventId, request.userId());

            return new LoadTestIssueResponse(
                    true,
                    "SUCCESS",
                    "쿠폰 발급 성공"
            );

        } catch (BaseException e) {
            return new LoadTestIssueResponse(
                    false,
                    String.valueOf(e.getStatus().getCode()),
                    e.getStatus().getMessage()
            );
        }
    }

    @PostMapping("/{eventId}/issues/redis")
    public LoadTestIssueResponse issueWithRedis(
            @PathVariable Long eventId,
            @RequestBody LoadTestIssueRequest request
    ) {
        try {
            couponIssueService.issueWithRedis(eventId, request.userId());

            return new LoadTestIssueResponse(
                    true,
                    "SUCCESS",
                    "쿠폰 발급 성공"
            );

        } catch (BaseException e) {
            return new LoadTestIssueResponse(
                    false,
                    String.valueOf(e.getStatus().getCode()),
                    e.getStatus().getMessage()
            );
        }
    }

    @PostMapping("/{eventId}/issues/async")
    public LoadTestIssueResponse issueAsync(
            @PathVariable Long eventId,
            @RequestBody LoadTestIssueRequest request
    ) {
        try {
            couponIssueService.issueAsync(eventId, request.userId());

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
