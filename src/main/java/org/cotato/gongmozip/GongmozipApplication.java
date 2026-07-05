package org.cotato.gongmozip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class GongmozipApplication {

    public static void main(String[] args) {
        SpringApplication.run(GongmozipApplication.class, args);
    }
}
