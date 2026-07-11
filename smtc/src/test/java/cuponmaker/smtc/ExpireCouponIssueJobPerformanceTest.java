package cuponmaker.smtc;

import cuponmaker.smtc.domain.issue.entity.CouponIssue;
import cuponmaker.smtc.domain.issue.entity.CouponIssueStatus;
import cuponmaker.smtc.domain.issue.repository.CouponIssueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ExpireCouponIssueJobPerformanceTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job expireCouponIssueJob;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @BeforeEach
    void setUp() {
        couponIssueRepository.deleteAllInBatch();
    }

    @Test
    void 만료_쿠폰_5000건을_EXPIRED로_변경한다() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.of(2026, 7, 11, 3, 0);

        int expiredTargetCount = 5_000;
        int validIssuedCount = 5_000;

        List<CouponIssue> issues = new ArrayList<>();

        for (int i = 0; i < expiredTargetCount; i++) {
            issues.add(new CouponIssue(
                    1L,
                    (long) i + 1,
                    now.minusDays(10),
                    now.minusDays(1)
            ));
        }

        for (int i = 0; i < validIssuedCount; i++) {
            issues.add(new CouponIssue(
                    2L,
                    (long) i + 10_001,
                    now.minusDays(1),
                    now.plusDays(1)
            ));
        }

        couponIssueRepository.saveAll(issues);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("now", now.toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // when
        long start = System.currentTimeMillis();

        JobExecution jobExecution = jobLauncher.run(expireCouponIssueJob, jobParameters);

        long end = System.currentTimeMillis();
        long elapsedMs = end - start;

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        long expiredCount = couponIssueRepository.countByStatus(CouponIssueStatus.EXPIRED);
        long issuedCount = couponIssueRepository.countByStatus(CouponIssueStatus.ISSUED);

        assertThat(expiredCount).isEqualTo(expiredTargetCount);
        assertThat(issuedCount).isEqualTo(validIssuedCount);

        System.out.println("Batch elapsed ms = " + elapsedMs);
        System.out.println("Batch elapsed sec = " + elapsedMs / 1000.0);
        System.out.println("EXPIRED count = " + expiredCount);
        System.out.println("ISSUED count = " + issuedCount);

        jobExecution.getStepExecutions().forEach(stepExecution -> {
            System.out.println("step = " + stepExecution.getStepName());
            System.out.println("readCount = " + stepExecution.getReadCount());
            System.out.println("writeCount = " + stepExecution.getWriteCount());
            System.out.println("filterCount = " + stepExecution.getFilterCount());
            System.out.println("commitCount = " + stepExecution.getCommitCount());
        });
    }
}
