package com.projeto.dto;

import java.time.Instant;
import java.util.List;

import com.projeto.entity.enums.AcaoPermissao;
import com.projeto.entity.enums.AuthorityFuncao;

public class FuncaoDTO {

	public record Atribuicao(
			Long restauranteId,
			List<AuthorityFuncao> conceder,
			List<AuthorityFuncao> revogar,
			List<Long> cargos,
			List<AuthorityFuncao> authorities,
			List<Long> revogarCargos,
			List<AuthorityFuncao> revogarAuthorities) {

		public Atribuicao(Long restauranteId, List<AuthorityFuncao> conceder, List<AuthorityFuncao> revogar) {
			this(restauranteId, conceder, revogar, List.of(), List.of(), List.of(), List.of());
		}
	}

	public record Linha(AuthorityFuncao authority, Long atribuidoPor, Instant atribuidoEm, Long cargoOrigemId) {

		public Linha(AuthorityFuncao authority, Long atribuidoPor, Instant atribuidoEm) {
			this(authority, atribuidoPor, atribuidoEm, null);
		}
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
			String authority,
			AcaoPermissao acao,
			String autorNome,
			String alvoNome,
			String restauranteNome) {
	}

	public record CargoItem(Long id, String nome, String descricao, boolean ativo, List<AuthorityFuncao> authorities) {
	}

	public record CargoCriar(String nome, String descricao, List<AuthorityFuncao> authorities) {
	}

	public record CargoAtualizar(String nome, String descricao, Boolean ativo, List<AuthorityFuncao> authorities) {
	}
}
