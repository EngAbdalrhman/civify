package com.app.civify.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for notification system.
 * 
 * Architecture:
 * 1. notification.fanout - Broadcasts to all users
 * 2. notification.direct - User-specific notifications
 * 3. Queues: websocket, email, fcm (each consumer their own queue)
 * 4. Dead Letter Exchange for failed messages
 */
@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String NOTIFICATION_FANOUT_EXCHANGE = "notification.fanout";
    public static final String NOTIFICATION_DIRECT_EXCHANGE = "notification.direct";
    public static final String NOTIFICATION_DLX = "notification.dlx"; // Dead Letter Exchange

    // Queue names
    public static final String WEBSOCKET_QUEUE = "notification.websocket";
    public static final String EMAIL_QUEUE = "notification.email";
    public static final String FCM_QUEUE = "notification.fcm";
    public static final String DLQ = "notification.dlq"; // Dead Letter Queue

    // Routing keys
    public static final String WEBSOCKET_ROUTING_KEY = "notification.websocket";
    public static final String EMAIL_ROUTING_KEY = "notification.email";
    public static final String FCM_ROUTING_KEY = "notification.fcm";

    // ========================
    // Exchanges
    // ========================

    @Bean
    public FanoutExchange notificationFanoutExchange() {
        return new FanoutExchange(NOTIFICATION_FANOUT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange notificationDirectExchange() {
        return new DirectExchange(NOTIFICATION_DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(NOTIFICATION_DLX, true, false);
    }

    // ========================
    // Queues
    // ========================

    @Bean
    public Queue websocketQueue() {
        return QueueBuilder.durable(WEBSOCKET_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue fcmQueue() {
        return QueueBuilder.durable(FCM_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    // ========================
    // Bindings
    // ========================

    // Fanout bindings (broadcast to all queues)
    @Bean
    public Binding websocketFanoutBinding(Queue websocketQueue, FanoutExchange notificationFanoutExchange) {
        return BindingBuilder.bind(websocketQueue).to(notificationFanoutExchange);
    }

    @Bean
    public Binding emailFanoutBinding(Queue emailQueue, FanoutExchange notificationFanoutExchange) {
        return BindingBuilder.bind(emailQueue).to(notificationFanoutExchange);
    }

    @Bean
    public Binding fcmFanoutBinding(Queue fcmQueue, FanoutExchange notificationFanoutExchange) {
        return BindingBuilder.bind(fcmQueue).to(notificationFanoutExchange);
    }

    // Direct bindings (user-specific notifications)
    @Bean
    public Binding websocketDirectBinding(Queue websocketQueue, DirectExchange notificationDirectExchange) {
        return BindingBuilder.bind(websocketQueue).to(notificationDirectExchange).with(WEBSOCKET_ROUTING_KEY);
    }

    @Bean
    public Binding emailDirectBinding(Queue emailQueue, DirectExchange notificationDirectExchange) {
        return BindingBuilder.bind(emailQueue).to(notificationDirectExchange).with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding fcmDirectBinding(Queue fcmQueue, DirectExchange notificationDirectExchange) {
        return BindingBuilder.bind(fcmQueue).to(notificationDirectExchange).with(FCM_ROUTING_KEY);
    }

    // Dead Letter Queue binding
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("dlq");
    }

    // ========================
    // RabbitTemplate Configuration
    // ========================

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
