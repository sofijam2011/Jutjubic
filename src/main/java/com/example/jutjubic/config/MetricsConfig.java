package com.example.jutjubic.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MetricsConfig {

    @Bean
    public MeterBinder activeUsersMetrics() {
        return (registry) -> {

        };
    }
}
