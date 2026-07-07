package cuponmaker.smtc.domain.issue.controller;

import cuponmaker.smtc.domain.issue.dto.CouponIssueResponse;
import cuponmaker.smtc.domain.issue.service.CouponIssueService;
import cuponmaker.smtc.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Coupon Issue", description = "쿠폰 발급 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CouponIssueController {

    private final CouponIssueService couponIssueService;

    @Operation(summary = "쿠폰 발급 요청")
    @PostMapping("/coupon-events/{eventId}/issues")
    public CouponIssueResponse issue(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return couponIssueService.issue(eventId, userDetails.getUserId());
    }

    @Operation(summary = "내 쿠폰 발급 내역 조회")
    @GetMapping("/me/coupon-issues")
    public List<CouponIssueResponse> findMyIssues(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return couponIssueService.findMyIssues(userDetails.getUserId());
    }

    @Operation(summary = "특정 이벤트 내 발급 결과 조회")
    @GetMapping("/coupon-events/{eventId}/issues/me")
    public CouponIssueResponse findMyIssueByEvent(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return couponIssueService.findMyIssueByEvent(eventId, userDetails.getUserId());
    }
}
