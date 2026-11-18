package com.projeto.dto;

import java.util.List;

import com.projeto.entity.enums.StatusRestaurante;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class TiDTO {

	private TiDTO() {
	}

	public record Create(
			@NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
			@NotBlank(message = "Informe o e-mail") @Email(message = "Informe um e-mail válido") @Size(max = 255) String email,
			@NotBlank(message = "Informe a senha") @Size(min = 6, max = 72, message = "A senha precisa ter entre 6 e 72 caracteres") String senha) {
	}

	public record RestauranteResumo(Long id, String nome, StatusRestaurante status) {
	}

	public record Admin(Long id, String nome, String email, boolean ativo, List<RestauranteResumo> restaurantes) {
	}

	public record UsuarioTi(Long id, String nome, String email, boolean ativo, boolean principal) {
	}
}
