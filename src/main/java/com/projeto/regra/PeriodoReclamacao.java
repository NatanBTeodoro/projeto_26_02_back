package com.projeto.regra;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.projeto.util.exception.RegraDeNegocioException;

public final class PeriodoReclamacao {

	public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

	private PeriodoReclamacao() {
	}

	public record Limites(Instant inicio, Instant fim) {
	}

	public static Limites de(LocalDate inicio, LocalDate fim) {
		if (inicio != null && fim != null && inicio.isAfter(fim)) {
			throw new RegraDeNegocioException("A data inicial não pode ser depois da final");
		}
		Instant de = inicio == null ? Instant.EPOCH : inicio.atStartOfDay(FUSO).toInstant();
		Instant ate = fim == null
				? Instant.parse("9999-12-31T00:00:00Z")
				: fim.plusDays(1).atStartOfDay(FUSO).toInstant();
		return new Limites(de, ate);
	}
}
