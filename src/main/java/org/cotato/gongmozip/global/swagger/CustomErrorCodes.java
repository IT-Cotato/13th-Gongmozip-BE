package org.cotato.gongmozip.global.swagger;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
// ErrorCodeOperationCustomizer가 런타임 리플렉션으로 읽으므로 RUNTIME 필수 (CLASS/SOURCE로 바꾸면 애노테이션이 안 보임)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CustomErrorCodes {
    // 해당 엔드포인트 도메인에 특화된 에러 코드 enum (DomainErrorCode)
    Class<?>[] domainErrorCodes() default {};
    // 인증·서버 오류 등 전 엔드포인트 공통 에러 코드 enum (예: GlobalErrorCode)
    Class<?>[] commonErrorCodes() default {};
}
