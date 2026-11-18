package com.projeto.entity.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.projeto.util.exception.RegraDeNegocioException;

class AssuntoGestorTest {

	@Test
	void problemaNoSistemaSegueParaOTi() {
		assertEquals(DestinoReclamacao.TI, AssuntoGestor.PROBLEMA_NO_SISTEMA.destino());
		assertEquals("Problema no Sistema", AssuntoGestor.PROBLEMA_NO_SISTEMA.rotulo());
	}

	@Test
	void demaisAssuntosSeguemParaOAdministrador() {
		assertEquals(DestinoReclamacao.ADMINISTRADOR, AssuntoGestor.CARDAPIO.destino());
		assertEquals(DestinoReclamacao.ADMINISTRADOR, AssuntoGestor.PEDIDOS.destino());
		assertEquals(DestinoReclamacao.ADMINISTRADOR, AssuntoGestor.EQUIPE.destino());
		assertEquals(DestinoReclamacao.ADMINISTRADOR, AssuntoGestor.OUTRO.destino());
	}

	@Test
	void assuntoDesconhecidoEErro() {
		RegraDeNegocioException erro = assertThrows(
				RegraDeNegocioException.class,
				() -> AssuntoGestor.exigir("Problema no Sistema"));
		assertEquals("Escolha um assunto válido", erro.getMessage());
	}
}
