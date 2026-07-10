package cuponmaker.smtc.domain.issue.service;

import cuponmaker.smtc.global.exception.BaseException;
import cuponmaker.smtc.global.response.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponIssueRedisService {

    private final StringRedisTemplate redisTemplate;
    private static final long ROLLBACK_MARKER_TTL_SECONDS = 60 * 60 * 24; // 이벤트 기간 + 버퍼 고려해서 조정

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

    public void reserve(Long eventId, Long userId) {
        String userKey = getUserKey(eventId);
        String remainingStockKey = getRemainingStockKey(eventId);

        Long added = redisTemplate.opsForSet()
                .add(userKey, String.valueOf(userId));

        if (added == null || added == 0) {
            throw new BaseException(BaseResponseStatus.COUPON_ALREADY_ISSUED);
        }

        Long remainingStock = redisTemplate.opsForValue()
                .decrement(remainingStockKey);

        if (remainingStock == null) {
            rollbackUserOnly(eventId, userId);
            throw new BaseException(BaseResponseStatus.COUPON_EVENT_NOT_FOUND);
        }

        if (remainingStock < 0) {
            cancelReservation(eventId, userId);
            throw new BaseException(BaseResponseStatus.COUPON_SOLD_OUT);
        }
    }

    private void rollbackUserOnly(Long eventId, Long userId) {
        redisTemplate.opsForSet()
                .remove(getUserKey(eventId), String.valueOf(userId));
    }


    public void confirmIssue(Long eventId) {
        redisTemplate.opsForValue()
                .increment(getIssuedCountKey(eventId));
    }

    public void rollbackIfNotAlready(Long eventId, Long userId) {
        String markerKey = getRollbackMarkerKey(eventId, userId);

        // SETNX: 마커가 없을 때만 SET에 성공 (true) → 최초 1회만 진입
        Boolean firstAttempt = redisTemplate.opsForValue()
                .setIfAbsent(markerKey, "1", Duration.ofSeconds(ROLLBACK_MARKER_TTL_SECONDS));

        if (Boolean.FALSE.equals(firstAttempt)) {
            log.warn("이미 롤백 처리된 요청, 스킵. eventId={}, userId={}", eventId, userId);
            return;
        }

        // 최초 1회 진입 성공 시에만 실제 원복 수행
        cancelReservation(eventId, userId);
    }

    private String getRollbackMarkerKey(Long eventId, Long userId) {
        return "coupon:" + eventId + ":rollback:" + userId;
    }

    public void cancelReservation(Long eventId, Long userId) {
        redisTemplate.opsForValue()
                .increment(getRemainingStockKey(eventId));

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

    public Long getRemainingStock(Long eventId) {
        String value = redisTemplate.opsForValue()
                .get(getRemainingStockKey(eventId));

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

        redisTemplate.delete(getIssueInfoKey(eventId));
        redisTemplate.delete(getRemainingStockKey(eventId));
        redisTemplate.delete(getUserKey(eventId));
        redisTemplate.delete(getIssuedCountKey(eventId));

    }

    private String getIssueInfoKey(Long eventId) {
        return "coupon:" + eventId + ":issue-info";
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

    private String getRemainingStockKey(Long eventId) {
        return "coupon:" + eventId + ":remaining-stock";
    }

    public void initializeStock(Long eventId, int totalQuantity, LocalDateTime endAt) {
        String key = getRemainingStockKey(eventId);

        redisTemplate.opsForValue()
                .set(key, String.valueOf(totalQuantity));

        Duration ttl = Duration.between(
                LocalDateTime.now(),
                endAt.plusDays(1)
        );

        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.expire(key, ttl);
        }
    }

}
