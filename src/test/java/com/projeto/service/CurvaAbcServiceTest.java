package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CurvaAbcServiceTest {

	@Test
	void semVendaAnteriorNaoCalculaVariacao() {
		CurvaAbcService.VariacaoLucro variacao = CurvaAbcService.variacao(true, new BigDecimal("10"), null);

		assertNull(variacao.percentual());
		assertEquals("Sem período anterior", variacao.texto());
	}

	@Test
	void lucroNovoQuandoOAnteriorEraZero() {
		CurvaAbcService.VariacaoLucro variacao = CurvaAbcService.variacao(false, new BigDecimal("10"), BigDecimal.ZERO);

		assertNull(variacao.percentual());
		assertEquals("Novo", variacao.texto());
	}

	@Test
	void variacaoUsaOLucroDoPeriodoAnterior() {
		CurvaAbcService.VariacaoLucro variacao = CurvaAbcService.variacao(
				false, new BigDecimal("6.00"), new BigDecimal("10.00"));

		assertEquals(new BigDecimal("-40.00"), variacao.percentual());
		assertNull(variacao.texto());
	}
}
