package com.projeto.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.projeto.regra.RegraSenha;

public class SenhaValidador implements ConstraintValidator<SenhaValida, String> {

	@Override
	public boolean isValid(String senha, ConstraintValidatorContext context) {
		if (senha == null || senha.isBlank()) {
			return true;
		}
		return RegraSenha.aceita(senha);
	}
}
