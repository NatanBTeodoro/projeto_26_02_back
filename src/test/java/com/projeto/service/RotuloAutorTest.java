package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.projeto.entity.enums.Perfil;

class RotuloAutorTest {

	@Test
	void adminDiretoFicaAdminEPedidoComoUsuarioNao() {
		assertEquals("Admin", RotuloAutor.direto(Perfil.ADMINISTRADOR, "Ana", "ana@admin.com"));
		assertNull(RotuloAutor.noPedido(Perfil.ADMINISTRADOR, "Ana", "ana@admin.com"));
	}

	@Test
	void gestorLevaONomeJuntoComOPapel() {
		assertEquals("Bia · Gestor", RotuloAutor.direto(Perfil.GESTOR, "Bia", "bia@casa.com"));
		assertEquals("Bia · Gestor", RotuloAutor.noPedido(Perfil.GESTOR, "Bia", "bia@casa.com"));
	}

	@Test
	void usuarioMantemOProprioNome() {
		assertEquals("Caio", RotuloAutor.direto(Perfil.USUARIO, "Caio", "caio@casa.com"));
		assertNull(RotuloAutor.noPedido(Perfil.USUARIO, "Caio", "caio@casa.com"));
	}
}
