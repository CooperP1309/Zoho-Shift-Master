package com.example.app.scheduler.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class SchedulerConfiguration
{
    /*  Source of "now" for the scheduler
     *
     *  Injected rather than calling ZonedDateTime.now() directly
     *  so tests can pin the time with Clock.fixed(...).
     */
    @Bean
    public Clock clock(@Value("Australia/Sydney") String zone)
    {
        return Clock.system(ZoneId.of(zone));
    }
}