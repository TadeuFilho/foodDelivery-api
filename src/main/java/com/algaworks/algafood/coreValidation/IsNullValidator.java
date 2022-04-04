package com.algaworks.algafood.coreValidation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.http.ResponseEntity;

public class IsNullValidator implements ConstraintValidator<IsNull,String> {

    @Override
    public boolean isValid(String valueToCheck, ConstraintValidatorContext context) {
        if(valueToCheck.isBlank()) {
            ResponseEntity.badRequest().build();
            return false;
        }
        return true;
    }

}

