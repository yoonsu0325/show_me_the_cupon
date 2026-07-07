package cuponmaker.smtc.domain.coupon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CouponEventCreateRequest(

        @NotBlank(message = "쿠폰 이벤트명은 필수입니다.")
        @Size(max = 100, message = "쿠폰 이벤트명은 100자 이하로 입력해주세요.")
        String title,

        @Size(max = 500, message = "설명은 500자 이하로 입력해주세요.")
        String description,

        @Min(value = 1, message = "쿠폰 수량은 1개 이상이어야 합니다.")
        int totalQuantity,

        @Min(value = 1, message = "쿠폰 유효기간은 1일 이상이어야 합니다.")
        int validDays,

        @NotNull(message = "발급 시작 시간은 필수입니다.")
        LocalDateTime startAt,

        @NotNull(message = "발급 종료 시간은 필수입니다.")
        LocalDateTime endAt
) {
}
