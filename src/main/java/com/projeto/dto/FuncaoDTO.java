package com.projeto.dto;

import java.time.Instant;
import java.util.List;

import com.projeto.entity.enums.AcaoPermissao;
import com.projeto.entity.enums.AuthorityFuncao;

public class FuncaoDTO {

	public record Atribuicao(
			Long restauranteId,
			List<AuthorityFuncao> conceder,
			List<AuthorityFuncao> revogar) {
	}

	public record Linha(AuthorityFuncao authority, Long atribuidoPor, Instant atribuidoEm) {
	}

	public record CatalogoCriar(String rotulo, String descricao, List<AuthorityFuncao> authorities) {
	}

	public record CatalogoAtualizar(String rotulo, String descricao, Boolean ativo) {
	}

	public record CatalogoItem(Long id, String rotulo, String descricao, AuthorityFuncao authority, boolean ativo) {
	}

	public record AuditoriaItem(
			Long id,
			Instant dataHora,
			Long autorId,
			Long alvoId,
			Long restauranteId,
			AuthorityFuncao authority,
			AcaoPermissao acao) {
	}
}
