package com.projeto.regra;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegraSenhaTest {

	@Test
	void aceitaSenhaComMaiusculaNumeroEEspecial() {
		assertTrue(RegraSenha.aceita("Casa1!"));
	}

	@Test
	void recusaSenhaCurtaSemMaiusculaSemNumeroOuSemEspecial() {
		assertFalse(RegraSenha.aceita("Ab1!"));
		assertFalse(RegraSenha.aceita("casa1!"));
		assertFalse(RegraSenha.aceita("Casa!!"));
		assertFalse(RegraSenha.aceita("Casa12"));
	}
}
