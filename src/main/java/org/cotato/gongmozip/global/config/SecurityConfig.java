package org.cotato.gongmozip.global.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.security.jwt.JwtAuthenticationEntryPoint;
import org.cotato.gongmozip.global.security.jwt.JwtAuthenticationFilter;
import org.cotato.gongmozip.global.security.oauth2.handler.OAuth2AuthenticationFailureHandler;
import org.cotato.gongmozip.global.security.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import org.cotato.gongmozip.global.security.oauth2.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                                "/api/auth/password-reset/code", // 비밀번호 재설정 인증코드 전송
                                "/api/auth/password-reset/verify", // 비밀번호 재설정 인증코드 확인
                                "/api/auth/password-reset", // 비밀번호 재설정
                                "/login/oauth2/**",
                                "/oauth2/**", // 소셜 로그인
                                "/swagger-ui/**",
                                "/v3/api-docs/**", // swagger
                                "/ws/**", // WebSocket(STOMP) 핸드셰이크 - 인증은 CONNECT 프레임에서 별도 처리
                                "/chat-test.html", // 로컬 수동 테스트용 정적 페이지
                                "/api/test/auth/quick-login", // [개발용] 이메일 인증 없이 토큰 발급
                                "/api/inquiries", // 문의 작성
                                "/api/inquiries/list", // 문의 내역 조회
                                "/api/inquiries/*" // 문의 상세 조회
                                )
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/contests")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/contests/*")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/contests/*")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/admin/uploads/presigned-url")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/admin/inquiries/**") // 문의하기 답변 관련 API
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                // 소셜 로그인
                .oauth2Login(
                        oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                                .successHandler(oAuth2AuthenticationSuccessHandler)
                                .failureHandler(oAuth2AuthenticationFailureHandler))
                // 인증 실패 시 401 응답 처리
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://13.209.254.149.nip.io",
                "https://13th-gongmozip-fe.vercel.app"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    // BCrypt 해시 알고리즘으로 비밀번호 암호화 (bean 등록)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
