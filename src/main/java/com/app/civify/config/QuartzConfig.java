package com.app.civify.config;

import com.app.civify.job.SubscriptionJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    private static final String SUBSCRIPTION_JOB_IDENTITY = "subscriptionJob";
    private static final String SUBSCRIPTION_TRIGGER_IDENTITY = "subscriptionTrigger";

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
        // Run every day at 00:01 AM
        return TriggerBuilder.newTrigger()
                .withIdentity(SUBSCRIPTION_TRIGGER_IDENTITY)
                .forJob(subscriptionJobDetail())
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 1))
                .build();
    }
}
