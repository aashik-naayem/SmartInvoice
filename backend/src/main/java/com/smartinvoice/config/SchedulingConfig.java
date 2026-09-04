package com.smartinvoice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on Spring's {@code @Scheduled} support for the recurring-invoice and
 * payment-reminder background jobs.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
