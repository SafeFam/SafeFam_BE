package com.gold.safefam.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsuncConfig {

    @Bean(name = "analysisNotificationExecutor")
    public Executor analysisNotificationExecutor() {
        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setThreadNamePrefix(
                "analysis-notification-"
        );

        // 평상시 유지할 스레드 개수
        executor.setCorePoolSize(2);

        // 알림이 몰렸을 때 늘어날 수 있는 최대 스레드 개수
        executor.setMaxPoolSize(4);

        // 모든 스레드가 사용 중일 때 대기시킬 작업 수
        executor.setQueueCapacity(100);

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);

        executor.initialize();
        return executor;
    }
}
