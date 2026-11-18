package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.projeto.util.exception.RegraDeNegocioException;

class PeriodoReclamacaoTest {

	@Test
	void diaNoFusoDeSaoPauloVaiAteODiaSeguinte() {
		PeriodoReclamacao.Limites limites = PeriodoReclamacao.de(
				LocalDate.of(2026, 11, 16),
				LocalDate.of(2026, 11, 16));

		assertEquals(Instant.parse("2026-11-16T03:00:00Z"), limites.inicio());
		assertEquals(Instant.parse("2026-11-17T03:00:00Z"), limites.fim());
	}

	@Test
	void semDatasAbreOIntervaloInteiro() {
		PeriodoReclamacao.Limites limites = PeriodoReclamacao.de(null, null);

		assertEquals(Instant.EPOCH, limites.inicio());
		assertEquals(Instant.parse("9999-12-31T00:00:00Z"), limites.fim());
	}

	@Test
	void dataInicialDepoisDaFinalEErro() {
		RegraDeNegocioException erro = assertThrows(
				RegraDeNegocioException.class,
				() -> PeriodoReclamacao.de(LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 1)));
		assertEquals("A data inicial não pode ser depois da final", erro.getMessage());
	}
}
