package cuponmaker.smtc;

import cuponmaker.smtc.domain.issue.entity.CouponIssue;
import cuponmaker.smtc.domain.issue.entity.CouponIssueStatus;
import cuponmaker.smtc.domain.issue.repository.CouponIssueRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ExpireCouponIssueJobTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job expireCouponIssueJob;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        couponIssueRepository.deleteAllInBatch();
    }

    @Test
    void 만료된_ISSUED_쿠폰만_EXPIRED로_변경된다() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 7, 11, 3, 0);

        CouponIssue expiredIssue = couponIssueRepository.save(
                new CouponIssue(
                        1L,
                        1L,
                        now.minusDays(3),
                        now.minusDays(1)
                )
        );

        CouponIssue validIssue = couponIssueRepository.save(
                new CouponIssue(
                        1L,
                        2L,
                        now.minusDays(1),
                        now.plusDays(1)
                )
        );

        //entityManager.flush();
        entityManager.clear();

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("now", now.toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncher.run(expireCouponIssueJob, jobParameters);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        entityManager.clear();

        CouponIssue foundExpiredIssue = couponIssueRepository.findById(expiredIssue.getId())
                .orElseThrow();

        CouponIssue foundValidIssue = couponIssueRepository.findById(validIssue.getId())
                .orElseThrow();

        assertThat(foundExpiredIssue.getStatus()).isEqualTo(CouponIssueStatus.EXPIRED);
        assertThat(foundValidIssue.getStatus()).isEqualTo(CouponIssueStatus.ISSUED);
    }
}
