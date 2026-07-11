package cuponmaker.smtc.global.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }
}
