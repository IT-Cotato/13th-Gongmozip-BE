package org.cotato.gongmozip.global.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "aiSummaryExecutor")
    public Executor aiSummaryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AI-Summary-Task-");
        executor.initialize();
        return executor;
    }

    // FCM 발송(외부 네트워크 I/O)이 요청 처리 스레드나 스케줄러 스레드를 막지 않도록 전용 풀을 둔다
    // (docs/decisions/13-fcm-push.md). 트랜잭션 커밋 이후에만 이 풀로 넘어간다.
    @Bean(name = "pushExecutor")
    public Executor pushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Push-Task-");
        executor.initialize();
        return executor;
    }
}
