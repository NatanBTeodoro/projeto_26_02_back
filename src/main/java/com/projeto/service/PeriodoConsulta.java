package com.projeto.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.projeto.util.exception.RegraDeNegocioException;

public final class PeriodoConsulta {

	private PeriodoConsulta() {
	}

	public record Intervalo(LocalDate de, LocalDate ate, Instant inicio, Instant fim) {
	}

	public static Intervalo exigir(LocalDate de, LocalDate ate) {
		if (de == null || ate == null) {
			throw new RegraDeNegocioException("Informe a data inicial e a data final");
		}
		if (de.isAfter(ate)) {
			throw new RegraDeNegocioException("A data inicial precisa ser anterior ou igual à final");
		}
		return new Intervalo(de, ate, inicio(de), fimExclusivo(ate));
	}

	public static Intervalo anterior(LocalDate de, LocalDate ate) {
		long dias = ChronoUnit.DAYS.between(de, ate) + 1;
		LocalDate ateAnterior = de.minusDays(1);
		LocalDate deAnterior = ateAnterior.minusDays(dias - 1);
		return new Intervalo(deAnterior, ateAnterior, inicio(deAnterior), fimExclusivo(ateAnterior));
	}

	public static Instant inicio(LocalDate dia) {
		return dia.atStartOfDay(AnaliseService.FUSO).toInstant();
	}

	public static Instant fimExclusivo(LocalDate dia) {
		return dia.plusDays(1).atStartOfDay(AnaliseService.FUSO).toInstant();
	}
}
