package cuponmaker.smtc;

import cuponmaker.smtc.domain.coupon.entity.CouponEvent;
import cuponmaker.smtc.domain.coupon.repository.CouponEventRepository;
import cuponmaker.smtc.domain.issue.repository.CouponIssueRepository;
import cuponmaker.smtc.domain.issue.service.CouponInfoIssueService;
import cuponmaker.smtc.domain.issue.service.CouponIssueRedisService;
import cuponmaker.smtc.domain.issue.service.CouponIssueService;
import cuponmaker.smtc.domain.user.entity.User;
import cuponmaker.smtc.domain.user.repository.UserRepository;
import cuponmaker.smtc.global.exception.BaseException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class CouponIssueConcurrencyTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    private CouponIssueService couponIssueService;

    @Autowired
    private CouponIssueRedisService couponIssueRedisService;

    @Autowired
    private CouponEventRepository couponEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Autowired
    private CouponInfoIssueService couponInfoIssueService;

    @Test
    void 동시에_여러명이_요청해도_쿠폰_수량만큼만_발급된다() throws InterruptedException {
        // given
        int totalQuantity = 100;
        int requestCount = 500;

        CouponEvent event = couponEventRepository.save(
                new CouponEvent(
                        "선착순 쿠폰",
                        "동시성 테스트용 쿠폰",
                        totalQuantity,
                        7,
                        LocalDateTime.now().minusMinutes(1),
                        LocalDateTime.now().plusDays(1)
                )
        );

        List<User> users = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            users.add(userRepository.save(
                    new User(
                            "user" + i + "@test.com",
                            "password",
                            "user" + i
                    )
            ));
        }

        System.out.println("[1] 유저 500명 생성 완료");

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(requestCount);
        System.out.println("[2] 동시성 테스트 시작");
        // when
        for (int i = 0; i < requestCount; i++) {
            Long userId = users.get(i).getId();

            executorService.submit(() -> {
                try {
                    couponIssueService.issue(event.getId(), userId);
                } catch (BaseException e) {
                    //System.out.println("[예외] 유저"+userId + " " + e.getStatus().getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        CouponEvent resultEvent = couponEventRepository.findById(event.getId()).orElseThrow();
        long issueCount = couponIssueRepository.countByCouponEventId(event.getId());

        assertThat(resultEvent.getIssuedQuantity()).isEqualTo(totalQuantity);
        assertThat(issueCount).isEqualTo(totalQuantity);
    }

    @Test
    void 같은_사용자가_동시에_여러번_요청해도_한번만_발급된다() throws InterruptedException {
        // given
        int totalQuantity = 100;
        int requestCount = 10;

        CouponEvent event = couponEventRepository.save(
                new CouponEvent(
                        "중복 발급 테스트 쿠폰",
                        "같은 사용자가 동시에 여러 번 요청하는 테스트",
                        totalQuantity,
                        7,
                        LocalDateTime.now().minusMinutes(1),
                        LocalDateTime.now().plusDays(1)
                )
        );

        User user = userRepository.save(
                new User(
                        "duplicate-user@test.com",
                        "password",
                        "중복테스트유저"
                )
        );

        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch latch = new CountDownLatch(requestCount);

        // when
        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    couponIssueService.issue(event.getId(), user.getId());
                } catch (BaseException e) {
                    System.out.println("[예외] 유저"+user.getId() + " " + e.getStatus().getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        CouponEvent resultEvent = couponEventRepository.findById(event.getId())
                .orElseThrow();

        long issueCount = couponIssueRepository.countByCouponEventIdAndUserId(
                event.getId(),
                user.getId()
        );

        assertThat(issueCount).isEqualTo(1);
        assertThat(resultEvent.getIssuedQuantity()).isEqualTo(1);
    }

    @Test
    void redis_쿠폰_동시_발급_테스트() throws InterruptedException {
        int totalQuantity = 100;
        int requestCount = 500;

        CouponEvent event = couponEventRepository.save(
                new CouponEvent(
                        "Redis 테스트 쿠폰",
                        "Redis 동시성 테스트",
                        totalQuantity,
                        30,
                        LocalDateTime.now().minusMinutes(1),
                        LocalDateTime.now().plusDays(1)
                )
        );

        couponIssueRedisService.clear(event.getId());

        List<User> users = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            users.add(userRepository.save(
                    new User(
                            "user" + i + "@test.com",
                            "password",
                            "user" + i
                    )
            ));
        }

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(requestCount);

        for (int i = 0; i < requestCount; i++) {
            Long userId = users.get(i).getId();

            executorService.submit(() -> {
                try {
                    couponIssueService.issueWithRedis(event.getId(), userId);
                } catch (BaseException ignored) {
                    System.out.println("[예외] 유저"+userId + " " + ignored.getStatus().getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long issueCount = couponIssueRepository.countByCouponEventId(event.getId());

        latch.await();

        entityManager.clear();

        CouponEvent savedEvent = couponEventRepository.findById(event.getId()).orElseThrow();

        Long reservedCount = couponIssueRedisService.getReservedCount(event.getId());
        Long issuedCount = couponIssueRedisService.getIssuedCount(event.getId());

        assertThat(issueCount).isEqualTo(totalQuantity);
        assertThat(savedEvent.getIssuedQuantity()).isEqualTo(totalQuantity);
        assertThat(reservedCount).isEqualTo(totalQuantity);
        assertThat(issuedCount).isEqualTo(totalQuantity);
    }

    @Test
    void redis_동일_유저_중복_발급_테스트() throws InterruptedException {
        int requestCount = 10;

        CouponEvent event = couponEventRepository.save(
                new CouponEvent(
                        "Redis 중복 테스트 쿠폰",
                        "Redis 중복 발급 테스트",
                        100,
                        30,
                        LocalDateTime.now().minusMinutes(1),
                        LocalDateTime.now().plusDays(1)
                )
        );

        User user = userRepository.save(
                new User(
                        "duplicate@test.com",
                        "password",
                        "duplicate"
                )
        );

        couponIssueRedisService.clear(event.getId());

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(requestCount);

        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    couponIssueService.issueWithRedis(event.getId(), user.getId());
                } catch (Exception ignored) {

                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long issueCount = couponIssueRepository.countByCouponEventIdAndUserId(
                event.getId(),
                user.getId()
        );

        Long reservedCount = couponIssueRedisService.getReservedCount(event.getId());
        Long issuedCount = couponIssueRedisService.getIssuedCount(event.getId());

        assertThat(issueCount).isEqualTo(1);
        assertThat(reservedCount).isEqualTo(1);
        assertThat(issuedCount).isEqualTo(1);
    }

    @Test
    void rabbitmq_비동기_쿠폰_동시_발급_테스트() throws InterruptedException {
        int totalQuantity = 100;
        int requestCount = 500;

        CouponEvent event = couponEventRepository.save(
                new CouponEvent(
                        "RabbitMQ 테스트 쿠폰",
                        "RabbitMQ 비동기 동시성 테스트",
                        totalQuantity,
                        30,
                        LocalDateTime.now().minusMinutes(1),
                        LocalDateTime.now().plusDays(1)
                )
        );

        couponIssueRedisService.clear(event.getId());

        couponInfoIssueService.saveIssueInfo(
                event.getId(),
                event.getStartAt(),
                event.getEndAt()
        );

        couponIssueRedisService.initializeStock(
                event.getId(),
                event.getTotalQuantity(),
                event.getEndAt()
        );

        List<User> users = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            users.add(userRepository.save(
                    new User(
                            "async-user" + i + "@test.com",
                            "password",
                            "async-user" + i
                    )
            ));
        }
        AtomicInteger acceptedCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();

        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(requestCount);

        for (int i = 0; i < requestCount; i++) {
            Long userId = users.get(i).getId();

            executorService.submit(() -> {
                try {
                    couponIssueService.issueAsync(event.getId(), userId);
                    acceptedCount.incrementAndGet();
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        System.out.println("accepted = " + acceptedCount.get());
        System.out.println("failed = " + failedCount.get());

        await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    long issueCount = couponIssueRepository.countByCouponEventId(event.getId());

                    entityManager.clear();

                    CouponEvent savedEvent = couponEventRepository.findById(event.getId())
                            .orElseThrow();

                    Long remainingStock = couponIssueRedisService.getRemainingStock(event.getId());
                    Long issuedCount = couponIssueRedisService.getIssuedCount(event.getId());

                    System.out.println("issued : " + issueCount + "  remain : "+ remainingStock + "  total : " + totalQuantity);

                    assertThat(issueCount).isEqualTo(totalQuantity);
                    assertThat(savedEvent.getIssuedQuantity()).isEqualTo(totalQuantity);
                    assertThat(remainingStock).isEqualTo(0L);
                    assertThat(issuedCount).isEqualTo(totalQuantity);
                });
    }
}
