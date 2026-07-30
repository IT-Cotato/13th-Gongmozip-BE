package org.cotato.gongmozip.global.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info().title("공모집").description("공모집 서비스 API 문서").version("0.1.0");

        // JWT 토큰 헤더 방식
        String securityScheme = "JWT TOKEN";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securityScheme);

        Components components = new Components()
                .addSecuritySchemes(
                        securityScheme,
                        new SecurityScheme()
                                .name(securityScheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("Bearer")
                                .bearerFormat("JWT"));

        return new OpenAPI()
                .info(info)
                // Swagger UI는 servers 목록의 첫 번째 항목을 기본 선택하므로 로컬 개발 편의를 위해
                // 로컬을 먼저 등록한다. 배포 서버로 테스트하려면 Swagger UI 상단 드롭다운에서 수동 선택.
                .addServersItem(new Server().url("http://localhost:8080").description("로컬"))
                .addServersItem(
                        new Server().url("https://13.209.254.149.nip.io").description("배포 서버"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
