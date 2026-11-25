package com.projeto.dto;

import java.math.BigDecimal;
import java.util.List;

import com.projeto.entity.enums.ClasseMenu;

public final class AnaliseDTO {

	private AnaliseDTO() {
	}

	public record Item(
			Long produtoId,
			String nome,
			String categoria,
			long quantidade,
			BigDecimal popularidade,
			BigDecimal margem,
			ClasseMenu classe,
			String recomendacao,
			BigDecimal margemPercentual,
			BigDecimal custoBruto) {

		public Item(
				Long produtoId,
				String nome,
				String categoria,
				long quantidade,
				BigDecimal popularidade,
				BigDecimal margem,
				ClasseMenu classe,
				String recomendacao,
				BigDecimal margemPercentual) {
			this(produtoId, nome, categoria, quantidade, popularidade, margem, classe, recomendacao, margemPercentual, null);
		}
	}

	public record Response(
			long quantidadePedidos,
			BigDecimal receita,
			BigDecimal ticketMedio,
			BigDecimal limitePopularidade,
			BigDecimal margemMedia,
			List<Item> itens,
			BigDecimal custoBruto,
			BigDecimal coberturaDeCusto) {

		public Response(
				long quantidadePedidos,
				BigDecimal receita,
				BigDecimal ticketMedio,
				BigDecimal limitePopularidade,
				BigDecimal margemMedia,
				List<Item> itens) {
			this(quantidadePedidos, receita, ticketMedio, limitePopularidade, margemMedia, itens, null, null);
		}
	}
}
