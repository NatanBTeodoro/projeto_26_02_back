package com.projeto.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CurvaAbc {

	public static final String CODIGO = "CURVA_ABC";

	private static final BigDecimal CEM = new BigDecimal("100.00");
	private static final BigDecimal ATE_A = new BigDecimal("80.00");
	private static final BigDecimal ATE_B = new BigDecimal("95.00");

	private CurvaAbc() {
	}

	public record Entrada(String id, String nome, BigDecimal lucro, boolean dadoIncompleto) {
	}

	public record Item(
			String id,
			String nome,
			BigDecimal lucro,
			BigDecimal participacao,
			BigDecimal acumulado,
			String faixa,
			boolean dadoIncompleto) {
	}

	public record Resultado(BigDecimal lucroBruto, List<Item> itens) {
	}

	public static Resultado calcular(List<Entrada> entradas) {
		List<Entrada> ordenadas = entradas.stream()
				.sorted(Comparator.comparing(Entrada::lucro).reversed()
						.thenComparing(Entrada::nome, String.CASE_INSENSITIVE_ORDER))
				.toList();
		BigDecimal total = ordenadas.stream()
				.map(Entrada::lucro)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
		List<Item> itens = new ArrayList<>();
		BigDecimal acumulado = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		for (int indice = 0; indice < ordenadas.size(); indice++) {
			Entrada entrada = ordenadas.get(indice);
			BigDecimal lucro = entrada.lucro().setScale(2, RoundingMode.HALF_UP);
			BigDecimal participacao = total.signum() == 0
					? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
					: lucro.multiply(CEM).divide(total, 2, RoundingMode.HALF_UP);
			String faixa = faixa(acumulado);
			acumulado = acumulado.add(participacao).setScale(2, RoundingMode.HALF_UP);
			if (indice == ordenadas.size() - 1 && total.signum() != 0) {
				acumulado = CEM;
			} else if (acumulado.compareTo(CEM) > 0) {
				acumulado = CEM;
			}
			itens.add(new Item(
					entrada.id(),
					entrada.nome(),
					lucro,
					participacao,
					acumulado,
					faixa,
					entrada.dadoIncompleto()));
		}
		return new Resultado(total, itens);
	}

	static String faixa(BigDecimal acumuladoAntes) {
		if (acumuladoAntes.compareTo(ATE_A) < 0) {
			return "A";
		}
		if (acumuladoAntes.compareTo(ATE_B) < 0) {
			return "B";
		}
		return "C";
	}
}
