package cuponmaker.smtc.global.config;

import cuponmaker.smtc.global.exception.BaseException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.util.Map;

import static cuponmaker.smtc.global.rabbitmq.RabbitMqProperties.*;

@Configuration
public class RabbitMqConfig {

    @Bean
    public Queue couponIssueQueue() {
        return QueueBuilder.durable(COUPON_ISSUE_QUEUE)
                .withArgument("x-dead-letter-exchange", COUPON_ISSUE_DLX)
                .withArgument("x-dead-letter-routing-key", COUPON_ISSUE_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public DirectExchange couponIssueDlx() {
        return new DirectExchange(COUPON_ISSUE_DLX);
    }

    @Bean
    public Queue couponIssueDlq() {
        return QueueBuilder.durable(COUPON_ISSUE_DLQ).build();
    }

    @Bean
    public Binding couponIssueDlqBinding() {
        return BindingBuilder
                .bind(couponIssueDlq())
                .to(couponIssueDlx())
                .with(COUPON_ISSUE_DLQ_ROUTING_KEY);
    }

    @Bean
    public DirectExchange couponIssueExchange() {
        return new DirectExchange(COUPON_ISSUE_EXCHANGE);
    }

    @Bean
    public Binding couponIssueBinding() {
        return BindingBuilder
                .bind(couponIssueQueue())
                .to(couponIssueExchange())
                .with(COUPON_ISSUE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    /**
     * 쿠폰 발급 consumer 용 기본 컨테이너 팩토리.
     * - 실패 시 최대 3회 재시도 (총 4회 시도, 지수 백오프 1s → 2s → 4s)
     * - 재시도 소진 시 RepublishMessageRecoverer 로 DLX 에 재발행 → DLQ 적재
     *   (x-exception-message, x-exception-stacktrace 등 실패 원인 헤더 포함)
     * - 비즈니스 예외(BaseException), 역직렬화 실패는 재시도해도 결과가 같으므로 즉시 DLQ 로 전달
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            RabbitTemplate rabbitTemplate
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        factory.setAdviceChain(couponIssueRetryInterceptor(rabbitTemplate));
        return factory;
    }

    /**
     * DLQ consumer 전용 팩토리.
     * 재시도/재발행 없이 실패 시 reject(requeue=false) 하여 DLQ ↔ DLX 무한 루프를 방지한다.
     */
    @Bean(name = DLQ_LISTENER_CONTAINER_FACTORY)
    public SimpleRabbitListenerContainerFactory dlqListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    private RetryOperationsInterceptor couponIssueRetryInterceptor(RabbitTemplate rabbitTemplate) {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = Map.of(
                BaseException.class, false,
                MessageConversionException.class, false
        );

        // traverseCauses=true: ListenerExecutionFailedException 으로 감싸진 원인 예외까지 검사
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                COUPON_ISSUE_MAX_ATTEMPTS,
                retryableExceptions,
                true,
                true
        );

        return RetryInterceptorBuilder.stateless()
                .retryPolicy(retryPolicy)
                .backOffOptions(RETRY_INITIAL_INTERVAL_MS, RETRY_MULTIPLIER, RETRY_MAX_INTERVAL_MS)
                .recoverer(new RepublishMessageRecoverer(
                        rabbitTemplate,
                        COUPON_ISSUE_DLX,
                        COUPON_ISSUE_DLQ_ROUTING_KEY
                ))
                .build();
    }
}
