package org.cotato.gongmozip.global.config;

import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.security.jwt.JwtAuthenticationEntryPoint;
import org.cotato.gongmozip.global.security.jwt.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

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
                                "/api/members/email/verify-request", // 인증코드 발송
                                "/api/members/email/verify", // 인증코드 확인
                                "/api/auth/login", // 로그인
                                "/api/auth/reissue", // 토큰 재발급
                                "/api/public/profiles/**", // 공개 프로필 조회 (비인증 허용)
                                "/swagger-ui/**",
                                "/v3/api-docs/**" // swagger
                                )
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                // 인증 실패 시 401 응답 처리
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    // BCrypt 해시 알고리즘으로 비밀번호 암호화 (bean 등록)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
