package com.projeto.dto;

import java.time.Instant;

import com.projeto.entity.Comanda;

public final class ComandaDTO {

	private ComandaDTO() {
	}

	public record Create(Long usuarioId) {
	}

	public record Response(
			Long id,
			Long restauranteId,
			Long usuarioId,
			String usuarioNome,
			Instant criadaEm,
			boolean aberta) {

		public static Response de(Comanda comanda) {
			Long usuarioId = comanda.getUsuario() == null ? null : comanda.getUsuario().getId();
			String usuarioNome = comanda.getUsuario() == null ? null : comanda.getUsuario().getNome();
			return new Response(
					comanda.getId(),
					comanda.getRestaurante().getId(),
					usuarioId,
					usuarioNome,
					comanda.getCriadaEm(),
					comanda.isAberta());
		}
	}
}
