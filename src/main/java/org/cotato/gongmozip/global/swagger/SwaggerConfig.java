package org.cotato.gongmozip.global.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("공모집 API")
                        .description("공모집 서비스 API 문서")
                        .version("v1.0.0")
                        .contact(new Contact().name("Cotato").email("cotato@example.com")));
    }
}
