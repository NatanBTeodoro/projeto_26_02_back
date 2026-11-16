package com.projeto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class SenhaDTO {

	private SenhaDTO() {
	}

	public record Troca(
			@NotBlank(message = "Informe a senha atual") String senhaAtual,
			@NotBlank(message = "Informe a nova senha") @Size(min = 6, max = 72, message = "A senha precisa ter entre 6 e 72 caracteres") String senhaNova) {
	}

	public record Redefinicao(
			@NotBlank(message = "Informe a nova senha") @Size(min = 6, max = 72, message = "A senha precisa ter entre 6 e 72 caracteres") String senhaNova) {
	}
}
