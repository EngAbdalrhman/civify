package com.app.civify.config;

import com.app.civify.job.SubscriptionJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class QuartzConfig {

    private static final String SUBSCRIPTION_JOB_IDENTITY = "subscriptionJob";
    private static final String SUBSCRIPTION_TRIGGER_IDENTITY = "subscriptionTrigger";

    @Value("${subscription.job.cron:0 1 0 * * ?}")
    private String subscriptionJobCron;

    @Bean
    public JobDetail subscriptionJobDetail() {
        return JobBuilder.newJob(SubscriptionJob.class)
                .withIdentity(SUBSCRIPTION_JOB_IDENTITY)
                .storeDurably()
                .withDescription("Daily job to handle subscription billing, downgrades, and resets")
                .build();
    }

    @Bean
    public Trigger subscriptionJobTrigger() {
        return TriggerBuilder.newTrigger()
                .withIdentity(SUBSCRIPTION_TRIGGER_IDENTITY)
                .forJob(subscriptionJobDetail())
                .withSchedule(CronScheduleBuilder.cronSchedule(subscriptionJobCron))
                .build();
    }
}
