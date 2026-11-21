package com.projeto.dto;

import java.math.BigDecimal;
import java.util.List;

public final class CurvaAbcDTO {

	private CurvaAbcDTO() {
	}

	public record Modelo(String codigo, String nome) {
	}

	public record Item(
			String id,
			String nome,
			BigDecimal lucro,
			BigDecimal participacao,
			BigDecimal acumulado,
			String faixa,
			boolean dadoIncompleto,
			Long quantidade,
			BigDecimal margemPercentual,
			String classe,
			BigDecimal variacaoLucro,
			String variacaoTexto) {
	}

	public record Response(
			String modelo,
			BigDecimal lucroBruto,
			long linhasSemCusto,
			List<String> foraDaCurva,
			List<Item> itens,
			BigDecimal faturamento,
			BigDecimal margemPercentual) {
	}
}
