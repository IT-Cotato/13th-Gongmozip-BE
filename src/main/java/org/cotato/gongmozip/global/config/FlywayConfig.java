package org.cotato.gongmozip.global.config;

import java.util.Arrays;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig implements BeanFactoryPostProcessor {

    // Spring Boot 4.x에서 FlywayAutoConfiguration이 제거되어 수동으로 실행 순서 보장
    // entityManagerFactory가 flyway bean보다 먼저 생성되면 ddl-auto: validate가 실패하므로
    // entityManagerFactory의 의존성에 flyway를 추가해 Flyway → JPA 순서 강제
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
            BeanDefinition jpaBean = beanFactory.getBeanDefinition("entityManagerFactory");
            String[] existing = jpaBean.getDependsOn();
            if (existing == null) {
                jpaBean.setDependsOn("flyway");
            } else {
                String[] updated = Arrays.copyOf(existing, existing.length + 1);
                updated[existing.length] = "flyway";
                jpaBean.setDependsOn(updated);
            }
        }
    }

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration") // migration SQL 파일 위치
                        .baselineOnMigrate(false) // flyway_schema_history 없을 때 baseline 생성 여부
                        .baselineVersion("0") // baseline 버전 (이 버전 이하 migration은 skip)
                        .outOfOrder(true) // V0 파일이 이미 적용된 V1보다 낮은 버전일 때 순서 무시하고 적용 허용
                        .load();
        // 이전 실행에서 실패한 마이그레이션 기록을 제거한 뒤 재실행
        // (반복 실행 환경에서 FAILED 상태로 남은 항목이 migrate를 막는 것을 방지)
        flyway.repair();
        flyway.migrate();
        return flyway;
    }
}
