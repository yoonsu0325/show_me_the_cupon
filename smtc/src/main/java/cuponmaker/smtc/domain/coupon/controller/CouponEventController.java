package cuponmaker.smtc.domain.coupon.controller;

import cuponmaker.smtc.domain.coupon.dto.CouponEventCreateRequest;
import cuponmaker.smtc.domain.coupon.dto.CouponEventResponse;
import cuponmaker.smtc.domain.coupon.service.CouponEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Coupon Event", description = "쿠폰 이벤트 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupon-events")
public class CouponEventController {

    private final CouponEventService couponEventService;

    @Operation(summary = "쿠폰 이벤트 생성")
    @PostMapping
    public CouponEventResponse create(
            @Valid @RequestBody CouponEventCreateRequest request
    ) {
        return couponEventService.create(request);
    }

    @Operation(summary = "쿠폰 이벤트 목록 조회")
    @GetMapping
    public List<CouponEventResponse> findAll() {
        return couponEventService.findAll();
    }

    @Operation(summary = "쿠폰 이벤트 상세 조회")
    @GetMapping("/{eventId}")
    public CouponEventResponse findById(@PathVariable Long eventId) {
        return couponEventService.findById(eventId);
    }
}
