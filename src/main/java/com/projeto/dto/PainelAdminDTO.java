package com.projeto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PainelAdminDTO {

	private PainelAdminDTO() {
	}

	public record Variacao(
			String sentido,
			String texto,
			BigDecimal percentual,
			BigDecimal pontos,
			BigDecimal anterior) {
	}

	public record Kpi(
			String codigo,
			String rotulo,
			String tipo,
			BigDecimal valor,
			String texto,
			String complemento,
			boolean atencao,
			List<String> detalhes,
			Variacao variacao) {
	}

	public record Fatia(
			String classe,
			int produtos,
			BigDecimal percentualProdutos,
			BigDecimal lucro,
			BigDecimal percentualLucro) {
	}

	public record Parte(String classe, int produtos, BigDecimal percentual) {
	}

	public record RestauranteSaude(
			long id,
			String nome,
			boolean amostraPequena,
			int produtosDistintos,
			long pedidos,
			BigDecimal percentualEstrelas,
			BigDecimal vendidosSemCusto,
			BigDecimal cardapioSemCusto,
			List<Parte> classes) {
	}

	public record Alerta(String severidade, String texto, BigDecimal impacto) {
	}

	public record Response(
			LocalDate de,
			LocalDate ate,
			LocalDate deAnterior,
			LocalDate ateAnterior,
			boolean semPeriodoAnterior,
			List<Kpi> kpis,
			List<Fatia> distribuicao,
			int restaurantesNaConsolidacao,
			List<RestauranteSaude> restaurantes,
			List<Alerta> alertas) {
	}
}
