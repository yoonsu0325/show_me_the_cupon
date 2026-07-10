package cuponmaker.smtc.domain.issue.service;

import cuponmaker.smtc.domain.issue.message.CouponIssueInfo;
import cuponmaker.smtc.global.exception.BaseException;
import cuponmaker.smtc.global.response.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponInfoIssueService {

    private static final String START_AT = "startAt";
    private static final String END_AT = "endAt";

    private final StringRedisTemplate redisTemplate;

    public void saveIssueInfo(
            Long eventId,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        String key = getIssueInfoKey(eventId);

        redisTemplate.opsForHash().put(key, START_AT, startAt.toString());
        redisTemplate.opsForHash().put(key, END_AT, endAt.toString());

        Duration ttl = Duration.between(
                LocalDateTime.now(),
                endAt.plusDays(1)
        );

        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.expire(key, ttl);
        }
    }

    public CouponIssueInfo getIssueInfo(Long eventId) {
        String key = getIssueInfoKey(eventId);

        Object startAt = redisTemplate.opsForHash().get(key, START_AT);
        Object endAt = redisTemplate.opsForHash().get(key, END_AT);

        if (startAt == null || endAt == null) {
            throw new BaseException(BaseResponseStatus.COUPON_EVENT_NOT_FOUND);
        }

        return new CouponIssueInfo(
                eventId,
                LocalDateTime.parse(startAt.toString()),
                LocalDateTime.parse(endAt.toString())
        );
    }

    public void deleteIssueInfo(Long eventId) {
        redisTemplate.delete(getIssueInfoKey(eventId));
    }

    private String getIssueInfoKey(Long eventId) {
        return "coupon:" + eventId + ":issue-info";
    }

}
