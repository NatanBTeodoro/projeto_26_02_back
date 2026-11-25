package com.projeto.regra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.projeto.util.exception.RegraDeNegocioException;

class RegrasComandaTest {

	@Test
	void comandaSemMesaAceitaAPrimeira() {
		RegrasComanda.exigirMesmaMesa(null, 4);
	}

	@Test
	void comandaNaoTrocaDeMesa() {
		RegraDeNegocioException erro = assertThrows(
				RegraDeNegocioException.class,
				() -> RegrasComanda.exigirMesmaMesa(4, 7));
		assertEquals("A comanda já está na mesa 4", erro.getMessage());
	}

	@Test
	void mesmoNomeEmOutraMesaUsaAMesaNaDistincao() {
		Set<String> colisao = RegrasComanda.nomesEmMaisDeUmaMesa(List.of(
				new RegrasComanda.Ocupacao("Ana", 2),
				new RegrasComanda.Ocupacao("ana", 5),
				new RegrasComanda.Ocupacao("Bia", 2),
				new RegrasComanda.Ocupacao("Bia", 2)));

		assertTrue(RegrasComanda.colide("Ana", colisao));
		assertFalse(RegrasComanda.colide("Bia", colisao));
		assertEquals("Ana · mesa 2", RegrasComanda.rotulo("Ana", 2, true));
		assertEquals("Bia", RegrasComanda.rotulo("Bia", 2, false));
	}
}
