package com.app.civify.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;

/**
 * Configuration for Google Cloud Pub/Sub integration.
 * Sets up the inbound channel adapter for pulling messages from the subscription.
 */
@Configuration
@EnableIntegration
@IntegrationComponentScan
public class PubSubConfig {

    @Value("${gcp.pubsub.subscription-name}")
    private String subscriptionName;

    @Bean
    public MessageChannel notificationInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter notificationInboundAdapter(
            MessageChannel notificationInputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter = 
            new PubSubInboundChannelAdapter(pubSubTemplate, subscriptionName);
        adapter.setOutputChannel(notificationInputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        adapter.setPayloadType(String.class);
        return adapter;
    }
}
