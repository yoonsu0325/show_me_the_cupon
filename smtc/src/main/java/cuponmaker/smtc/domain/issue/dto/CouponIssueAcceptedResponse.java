package cuponmaker.smtc.domain.issue.dto;

public record CouponIssueAcceptedResponse(
        Long eventId,
        String status,
        String message
) {
    public static CouponIssueAcceptedResponse of(Long eventId) {
        return new CouponIssueAcceptedResponse(
                eventId,
                "ACCEPTED",
                "쿠폰 발급 요청이 접수되었습니다."
        );
    }
}
