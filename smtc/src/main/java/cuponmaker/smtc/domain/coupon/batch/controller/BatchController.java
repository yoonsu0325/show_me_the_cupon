package cuponmaker.smtc.domain.coupon.batch.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/batch")
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job expireCouponIssueJob;

    @PostMapping("/expire-coupons")
    public String expireCoupons() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("now", LocalDateTime.now().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(expireCouponIssueJob, jobParameters);

        return "expireCouponIssueJob started";
    }
}
