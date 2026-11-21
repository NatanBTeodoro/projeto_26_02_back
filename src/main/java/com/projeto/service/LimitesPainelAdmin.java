package com.projeto.service;

import java.math.BigDecimal;

public final class LimitesPainelAdmin {

	public static final int MIN_PRODUTOS = 5;
	public static final int MIN_PEDIDOS = 10;
	public static final int DIAS_SEM_VENDA = 7;
	public static final BigDecimal DEPENDENCIA_ATENCAO = new BigDecimal("50");
	public static final BigDecimal DEPENDENCIA_CRITICO = new BigDecimal("70");
	public static final BigDecimal QUEDA_ATENCAO = new BigDecimal("20");
	public static final BigDecimal QUEDA_CRITICO = new BigDecimal("40");
	public static final BigDecimal DADOS_INCOMPLETOS = new BigDecimal("30");
	public static final BigDecimal MARGEM_ABAIXO = new BigDecimal("10");
	public static final BigDecimal VACA_LIMITE = new BigDecimal("60");
	public static final BigDecimal TETO_VARIACAO = new BigDecimal("999");

	private LimitesPainelAdmin() {
	}
}
