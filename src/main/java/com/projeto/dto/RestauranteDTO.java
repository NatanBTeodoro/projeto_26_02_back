package com.projeto.dto;

import com.projeto.entity.Restaurante;
import com.projeto.entity.enums.StatusRestaurante;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class RestauranteDTO {

	private RestauranteDTO() {
	}

	public record Create(
			@NotBlank(message = "Informe o nome") @Size(max = 150) String nome) {
	}

	public record Update(
			@NotBlank(message = "Informe o nome") @Size(max = 150) String nome) {
	}

	public record Response(Long id, String nome, StatusRestaurante status, Long adminId) {

		public static Response de(Restaurante restaurante) {
			return new Response(
					restaurante.getId(),
					restaurante.getNome(),
					restaurante.getStatus(),
					restaurante.getAdmin().getId());
		}
	}
}
