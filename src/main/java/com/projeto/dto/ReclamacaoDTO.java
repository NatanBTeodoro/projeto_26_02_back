package com.projeto.dto;

import java.time.Instant;

import com.projeto.entity.Reclamacao;
import com.projeto.entity.enums.DestinoReclamacao;
import com.projeto.entity.enums.StatusReclamacao;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ReclamacaoDTO {

	private ReclamacaoDTO() {
	}

	public record Item(
			Long id,
			String autorNome,
			String autorEmail,
			Instant criadoEm,
			String assunto,
			String descricao,
			StatusReclamacao status,
			String resposta,
			Instant respondidoEm,
			String anexoNome,
			DestinoReclamacao destino,
			String restauranteNome) {

		public static Item de(Reclamacao reclamacao) {
			return new Item(
					reclamacao.getId(),
					reclamacao.getAutorNome(),
					reclamacao.getAutorEmail(),
					reclamacao.getCriadoEm(),
					reclamacao.getAssunto(),
					reclamacao.getDescricao(),
					reclamacao.getStatus(),
					reclamacao.getResposta(),
					reclamacao.getRespondidoEm(),
					reclamacao.getAnexoNome(),
					reclamacao.getDestino(),
					reclamacao.getRestauranteNome());
		}
	}

	public record Atualizacao(
			@NotNull(message = "Informe o status") StatusReclamacao status,
			@Size(max = 4000, message = "A resposta passa de 4000 caracteres") String resposta) {
	}

	public record Anexo(String nome, String tipo, byte[] conteudo) {
	}
}
