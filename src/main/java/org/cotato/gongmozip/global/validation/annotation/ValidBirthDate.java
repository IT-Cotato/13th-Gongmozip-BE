package org.cotato.gongmozip.global.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.cotato.gongmozip.global.validation.BirthDateValidator;

@Documented
@Constraint(validatedBy = BirthDateValidator.class)
@Target({
    ElementType.FIELD,
    ElementType.METHOD,
    ElementType.PARAMETER,
    ElementType.ANNOTATION_TYPE,
    ElementType.TYPE_USE,
    ElementType.RECORD_COMPONENT
})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBirthDate {

    String message() default "유효하지 않은 생년월일입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int minimumAge() default 14;

    String futureMessage() default "생년월일은 미래일 수 없습니다.";

    String underAgeMessage() default "만 {minimumAge}세 이상만 가입할 수 있습니다.";
}
