package cuponmaker.smtc.domain.coupon.batch;

import cuponmaker.smtc.domain.issue.entity.CouponIssue;
import cuponmaker.smtc.domain.issue.entity.CouponIssueStatus;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ExpireCouponIssueJobConfig {

    private static final int CHUNK_SIZE = 100;

    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job expireCouponIssueJob(
            JobRepository jobRepository,
            Step expireCouponIssueStep
    ) {
        return new JobBuilder("expireCouponIssueJob", jobRepository)
                .start(expireCouponIssueStep)
                .build();
    }

    @Bean
    public Step expireCouponIssueStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JpaPagingItemReader<CouponIssue> expireCouponIssueReader,
            ItemProcessor<CouponIssue, CouponIssue> expireCouponIssueProcessor,
            ItemWriter<CouponIssue> expireCouponIssueWriter
    ) {
        return new StepBuilder("expireCouponIssueStep", jobRepository)
                .<CouponIssue, CouponIssue>chunk(CHUNK_SIZE, transactionManager)
                .reader(expireCouponIssueReader)
                .processor(expireCouponIssueProcessor)
                .writer(expireCouponIssueWriter)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<CouponIssue> expireCouponIssueReader(
            @Value("#{jobParameters['now']}") String now
    ) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("status", CouponIssueStatus.ISSUED);
        parameters.put("now", LocalDateTime.parse(now));

        return new JpaPagingItemReaderBuilder<CouponIssue>()
                .name("expireCouponIssueReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("""
                        select ci
                        from CouponIssue ci
                        where ci.status = :status
                          and ci.expiresAt < :now
                        order by ci.id asc
                        """)
                .parameterValues(parameters)
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<CouponIssue, CouponIssue> expireCouponIssueProcessor(
            @Value("#{jobParameters['now']}") String now
    ) {
        LocalDateTime jobNow = LocalDateTime.parse(now);

        return couponIssue -> {
            couponIssue.expire(jobNow);
            return couponIssue;
        };
    }

    @Bean
    public JpaItemWriter<CouponIssue> expireCouponIssueWriter() {
        JpaItemWriter<CouponIssue> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
