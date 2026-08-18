package org.cotato.gongmozip;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class GongmozipApplication {

    public static void main(String[] args) {
        // Spring 컨텍스트 초기화 전에 설정해야 한다. @PostConstruct에서 호출하면
        // 이미 HikariCP가 UTC로 커넥션을 만든 뒤라 기존 커넥션에는 반영되지 않는다.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(GongmozipApplication.class, args);
    }
}
