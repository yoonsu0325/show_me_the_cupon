package cuponmaker.smtc.domain.issue.service;

import cuponmaker.smtc.global.exception.BaseException;
import cuponmaker.smtc.global.response.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponIssueRedisService {

    private final StringRedisTemplate redisTemplate;

    public void reserve(Long eventId, Long userId, int totalQuantity) {

        String userKey = getUserKey(eventId);
        String reservedCountKey = getReservedCountKey(eventId);
        Long added = redisTemplate.opsForSet()
                .add(userKey, String.valueOf(userId));

        if (added == null || added == 0) {
            throw new BaseException(BaseResponseStatus.COUPON_ALREADY_ISSUED);
        }

        Long reservedCount = redisTemplate.opsForValue()
                .increment(reservedCountKey);

        if (reservedCount == null || reservedCount > totalQuantity) {
            cancelReservation(eventId, userId);
            throw new BaseException(BaseResponseStatus.COUPON_SOLD_OUT);
        }
    }

    public void confirmIssue(Long eventId) {
        redisTemplate.opsForValue()
                .increment(getIssuedCountKey(eventId));
    }

    public void cancelReservation(Long eventId, Long userId) {
        redisTemplate.opsForValue()
                .decrement(getReservedCountKey(eventId));

        redisTemplate.opsForSet()
                .remove(getUserKey(eventId), String.valueOf(userId));
    }

    public Long getReservedCount(Long eventId) {
        String value = redisTemplate.opsForValue()
                .get(getReservedCountKey(eventId));

        if (value == null) {
            return 0L;
        }

        return Long.parseLong(value);
    }

    public Long getIssuedCount(Long eventId) {
        String value = redisTemplate.opsForValue()
                .get(getIssuedCountKey(eventId));

        if (value == null) {
            return 0L;
        }

        return Long.parseLong(value);
    }

    public void clear(Long eventId) {
        redisTemplate.delete(getUserKey(eventId));
        redisTemplate.delete(getReservedCountKey(eventId));
        redisTemplate.delete(getIssuedCountKey(eventId));
    }

    private String getUserKey(Long eventId) {
        return "coupon:" + eventId + ":users";
    }

    private String getReservedCountKey(Long eventId) {
        return "coupon:" + eventId + ":reserved-count";
    }

    private String getIssuedCountKey(Long eventId) {
        return "coupon:" + eventId + ":issued-count";
    }

}
