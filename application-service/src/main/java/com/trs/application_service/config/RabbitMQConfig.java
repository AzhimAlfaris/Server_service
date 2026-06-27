package com.trs.application_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange sensorExchange(@Value("${app.rabbitmq.exchange:sensor.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public TopicExchange commandExchange(@Value("${app.rabbitmq.command-exchange:device.command.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue sensorRequestQueue(@Value("${app.rabbitmq.queue:sensor.request.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Queue sensorNotificationQueue(@Value("${app.rabbitmq.notification-queue:sensor.notification.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding sensorNotificationBinding(@Qualifier("sensorNotificationQueue") Queue sensorNotificationQueue,
                                             DirectExchange sensorExchange,
                                             @Value("${app.rabbitmq.notification-routing-key:sensor.notification}") String routingKey) {
        return BindingBuilder.bind(sensorNotificationQueue).to(sensorExchange).with(routingKey);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
