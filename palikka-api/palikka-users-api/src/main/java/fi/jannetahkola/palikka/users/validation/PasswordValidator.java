package fi.jannetahkola.palikka.users.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<Password, char[]> {
    private static final int MIN_SIZE = 6;
    private static final int MAX_SIZE = 24;

    @Override
    public boolean isValid(char[] chars, ConstraintValidatorContext constraintValidatorContext) {
        if (chars == null) {
            replaceDefaultMessage(constraintValidatorContext, "must not be null");
            return false;
        }
        if (chars.length < MIN_SIZE || chars.length > MAX_SIZE) {
            replaceDefaultMessage(constraintValidatorContext,
                    String.format("must be between %d and %d chars", MIN_SIZE, MAX_SIZE));
            return false;
        }
        boolean valid = true;
        for (char aChar : chars) {
            if (Character.isWhitespace(aChar)) {
                valid = false;
                replaceDefaultMessage(constraintValidatorContext, "must not contain whitespaces");
                break;
            }
        }
        return valid;
    }

    private void replaceDefaultMessage(ConstraintValidatorContext constraintValidatorContext, String newMessage) {
        constraintValidatorContext.disableDefaultConstraintViolation();
        constraintValidatorContext
                .buildConstraintViolationWithTemplate(newMessage)
                .addConstraintViolation();
    }
}
