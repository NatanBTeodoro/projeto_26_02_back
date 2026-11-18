package com.projeto.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = SenhaValidador.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaValida {

	String message() default "A senha precisa ter de 6 a 72 caracteres, com uma letra maiúscula, um número e um caractere especial";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
