package org.cotato.gongmozip.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    // 서버 OS 타임존과 무관하게 모든 서비스 정책을 한국 시간으로 통일
    public static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock systemClock() {
        return Clock.system(KOREA_ZONE_ID);
    }
}
