package com.projeto.dto;

import com.projeto.util.SenhaValida;

import jakarta.validation.constraints.NotBlank;

public final class SenhaDTO {

	private SenhaDTO() {
	}

	public record Troca(
			@NotBlank(message = "Informe a senha atual") String senhaAtual,
			@NotBlank(message = "Informe a nova senha") @SenhaValida String senhaNova) {
	}

	public record Redefinicao(
			@NotBlank(message = "Informe a nova senha") @SenhaValida String senhaNova) {
	}
}
