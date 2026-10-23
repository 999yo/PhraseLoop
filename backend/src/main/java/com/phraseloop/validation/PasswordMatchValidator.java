package com.phraseloop.validation;

import com.phraseloop.dto.RegisterRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if (request == null || request.password() == null || request.passwordConfirm() == null) {
            return true;
        }
        if (request.password().equals(request.passwordConfirm())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("パスワードが一致しません")
                .addPropertyNode("passwordConfirm")
                .addConstraintViolation();
        return false;
    }
}
