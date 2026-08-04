package org.cotato.gongmozip.global.config;

import javax.sql.DataSource;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링을 활성화하고 다중 인스턴스 환경에서 같은 작업이 중복 실행되지 않도록 DB 기반 잠금을 구성한다.
 * 애플리케이션 서버 시각 차이의 영향을 받지 않도록 잠금 만료 판단에는 DB 시각을 사용한다.
 */
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT100M")
public class SchedulingConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        // 서비스 DB의 shedlock 테이블을 모든 인스턴스가 공유해 분산 잠금 저장소로 사용한다.
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build());
    }
}
