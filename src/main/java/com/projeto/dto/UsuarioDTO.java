package com.projeto.dto;

import com.projeto.entity.Usuario;
import com.projeto.entity.enums.Perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UsuarioDTO {

	private UsuarioDTO() {
	}

	public record Create(
			@NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
			@NotBlank(message = "Informe o e-mail") @Email(message = "Informe um e-mail válido") @Size(max = 255) String email,
			@NotBlank(message = "Informe a senha") @Size(min = 6, max = 72, message = "A senha precisa ter entre 6 e 72 caracteres") String senha,
			@NotNull(message = "Informe o perfil") Perfil perfil,
			Long restauranteId) {
	}

	public record Update(
			@Size(max = 120) String nome,
			@Email(message = "Informe um e-mail válido") @Size(max = 255) String email,
			Boolean ativo,
			Perfil perfil) {
	}

	public record Sessao(
			Long id,
			String nome,
			String email,
			Perfil perfil,
			boolean ativo,
			String token,
			Long restauranteId,
			String restauranteNome) {

		public static Sessao de(Usuario usuario) {
			Long restauranteId = usuario.getRestaurante() == null ? null : usuario.getRestaurante().getId();
			String restauranteNome = usuario.getRestaurante() == null ? null : usuario.getRestaurante().getNome();
			return new Sessao(
					usuario.getId(),
					usuario.getNome(),
					usuario.getEmail(),
					usuario.getPerfil(),
					usuario.isAtivo(),
					usuario.getTokenAcesso(),
					restauranteId,
					restauranteNome);
		}
	}

	public record Response(
			Long id,
			String nome,
			String email,
			Perfil perfil,
			boolean ativo,
			Long restauranteId,
			String restauranteNome) {

		public static Response de(Usuario usuario) {
			Long restauranteId = usuario.getRestaurante() == null ? null : usuario.getRestaurante().getId();
			String restauranteNome = usuario.getRestaurante() == null ? null : usuario.getRestaurante().getNome();
			return new Response(
					usuario.getId(),
					usuario.getNome(),
					usuario.getEmail(),
					usuario.getPerfil(),
					usuario.isAtivo(),
					restauranteId,
					restauranteNome);
		}
	}
}
