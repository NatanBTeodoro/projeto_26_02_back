package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class CurvaAbcTest {

	@Test
	void ordenaDoMaiorLucroEMarcaFaixas() {
		CurvaAbc.Resultado resultado = CurvaAbc.calcular(List.of(
				new CurvaAbc.Entrada("c", "C", new BigDecimal("5.00"), false),
				new CurvaAbc.Entrada("a", "A", new BigDecimal("80.00"), false),
				new CurvaAbc.Entrada("b", "B", new BigDecimal("15.00"), false)));

		assertEquals(new BigDecimal("100.00"), resultado.lucroBruto());
		assertEquals(List.of("A", "B", "C"), resultado.itens().stream().map(CurvaAbc.Item::nome).toList());
		assertEquals(new BigDecimal("80.00"), resultado.itens().get(0).participacao());
		assertEquals(new BigDecimal("80.00"), resultado.itens().get(0).acumulado());
		assertEquals("A", resultado.itens().get(0).faixa());
		assertEquals(new BigDecimal("95.00"), resultado.itens().get(1).acumulado());
		assertEquals("B", resultado.itens().get(1).faixa());
		assertEquals(new BigDecimal("100.00"), resultado.itens().get(2).acumulado());
		assertEquals("C", resultado.itens().get(2).faixa());
	}

	@Test
	void itemQueCompletaAFaixaAContinuaEmA() {
		CurvaAbc.Resultado resultado = CurvaAbc.calcular(List.of(
				new CurvaAbc.Entrada("1", "Primeiro", new BigDecimal("70.00"), false),
				new CurvaAbc.Entrada("2", "Segundo", new BigDecimal("20.00"), false),
				new CurvaAbc.Entrada("3", "Terceiro", new BigDecimal("10.00"), false)));

		assertEquals("A", resultado.itens().get(0).faixa());
		assertEquals("A", resultado.itens().get(1).faixa());
		assertEquals("B", resultado.itens().get(2).faixa());
	}

	@Test
	void unicoItemFicaNaFaixaA() {
		CurvaAbc.Resultado resultado = CurvaAbc.calcular(List.of(
				new CurvaAbc.Entrada("1", "Unico", new BigDecimal("50.00"), false)));

		assertEquals("A", resultado.itens().get(0).faixa());
		assertEquals(new BigDecimal("100.00"), resultado.itens().get(0).acumulado());
	}
}
