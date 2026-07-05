package org.cotato.gongmozip.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // csrf 비활성화(stateless 방식 사용)
                .csrf(AbstractHttpConfigurer::disable)
                // html 폼 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                // http 기본 인증 방식 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                // stateless 방식 사용(jwt 토큰)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 접근 권한 설정
                .authorizeHttpRequests(auth -> auth.requestMatchers(
                                "/api/members/signup", // 회원 가입
                                "/swagger-ui/**", "/v3/api-docs/**" // swagger
                        )
                        .permitAll()
                        .anyRequest()
                        .authenticated());

        return http.build();
    }

    @Bean
    // BCrypt 해시 알고리즘으로 비밀번호 암호화 (bean 등록)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
