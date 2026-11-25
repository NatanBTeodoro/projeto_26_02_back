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
			Integer numero,
			Long restauranteId,
			Long usuarioId,
			String usuarioNome,
			Integer mesa,
			String nomeCliente,
			String rotulo,
			Instant criadaEm,
			boolean aberta) {

		public static Response de(Comanda comanda, String rotulo) {
			Long usuarioId = comanda.getUsuario() == null ? null : comanda.getUsuario().getId();
			String usuarioNome = comanda.getUsuario() == null ? null : comanda.getUsuario().getNome();
			return new Response(
					comanda.getId(),
					comanda.getNumero(),
					comanda.getRestaurante().getId(),
					usuarioId,
					usuarioNome,
					comanda.getMesa(),
					comanda.getNomeCliente(),
					rotulo,
					comanda.getCriadaEm(),
					comanda.isAberta());
		}
	}
}
