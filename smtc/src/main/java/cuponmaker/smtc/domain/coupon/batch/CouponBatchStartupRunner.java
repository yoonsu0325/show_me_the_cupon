package cuponmaker.smtc.domain.coupon.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponBatchStartupRunner implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final Job expireCouponIssueJob;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("서버 시작 시 만료 쿠폰 배치 실행 시작");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("now", LocalDateTime.now().toString())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(expireCouponIssueJob, jobParameters);

            log.info("서버 시작 시 만료 쿠폰 배치 실행 완료. status={}", jobExecution.getStatus());

        } catch (Exception e) {
            log.error("서버 시작 시 만료 쿠폰 배치 실행 실패", e);
        }
    }
}
