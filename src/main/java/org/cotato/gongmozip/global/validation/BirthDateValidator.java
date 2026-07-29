package org.cotato.gongmozip.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import org.cotato.gongmozip.global.validation.annotation.ValidBirthDate;

public class BirthDateValidator implements ConstraintValidator<ValidBirthDate, LocalDate> {

    private int minimumAge;
    private String futureMessage;
    private String underAgeMessage;

    @Override
    public void initialize(ValidBirthDate constraintAnnotation) {
        minimumAge = constraintAnnotation.minimumAge();
        futureMessage = constraintAnnotation.futureMessage();
        underAgeMessage = constraintAnnotation.underAgeMessage();
    }

    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
        if (birthDate == null) {
            return true;
        }

        LocalDate today = LocalDate.now(context.getClockProvider().getClock());
        if (birthDate.isAfter(today)) {
            return invalid(context, futureMessage);
        }
        if (birthDate.isAfter(today.minusYears(minimumAge))) {
            return invalid(context, underAgeMessage);
        }
        return true;
    }

    private boolean invalid(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
