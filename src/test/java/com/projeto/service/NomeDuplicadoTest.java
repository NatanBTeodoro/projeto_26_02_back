package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class NomeDuplicadoTest {

	@Test
	void primeiroNomeFicaLimpoERepetidoGanhaSufixo() {
		assertEquals("Maria", NomeDuplicado.reservar("Maria", List.of()));
		assertEquals("Maria#2", NomeDuplicado.reservar("Maria", List.of("Maria")));
		assertEquals("Maria#3", NomeDuplicado.reservar("Maria", List.of("Maria", "Maria#2")));
	}

	@Test
	void comparacaoIgnoraCaixaEMantemONomeDigitado() {
		assertEquals("maria#2", NomeDuplicado.reservar("maria", List.of("Maria")));
		assertEquals("João", NomeDuplicado.reservar("João", List.of("Maria")));
	}
}
