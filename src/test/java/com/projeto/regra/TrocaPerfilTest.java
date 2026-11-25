package com.projeto.regra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.projeto.entity.enums.Perfil;
import com.projeto.util.exception.RegraDeNegocioException;

class TrocaPerfilTest {

	@Test
	void adminTrocaUsuarioEGestorNosDoisSentidos() {
		TrocaPerfil.validar(Perfil.ADMINISTRADOR, 1L, 2L, Perfil.USUARIO, Perfil.GESTOR);
		TrocaPerfil.validar(Perfil.ADMINISTRADOR, 1L, 2L, Perfil.GESTOR, Perfil.USUARIO);
	}

	@Test
	void gestorNaoTrocaPerfil() {
		assertThrows(
				AccessDeniedException.class,
				() -> TrocaPerfil.validar(Perfil.GESTOR, 3L, 4L, Perfil.USUARIO, Perfil.GESTOR));
	}

	@Test
	void tiNaoTrocaEAdminNaoTrocaOProprio() {
		assertThrows(
				AccessDeniedException.class,
				() -> TrocaPerfil.validar(Perfil.TI, 3L, 4L, Perfil.USUARIO, Perfil.GESTOR));
		RegraDeNegocioException propria = assertThrows(
				RegraDeNegocioException.class,
				() -> TrocaPerfil.validar(Perfil.ADMINISTRADOR, 1L, 1L, Perfil.GESTOR, Perfil.USUARIO));
		assertEquals("Você não pode alterar o próprio perfil", propria.getMessage());
	}

	@Test
	void soAceitaUsuarioEGestor() {
		RegraDeNegocioException erro = assertThrows(
				RegraDeNegocioException.class,
				() -> TrocaPerfil.validar(Perfil.ADMINISTRADOR, 1L, 2L, Perfil.USUARIO, Perfil.TI));
		assertEquals("Perfil inválido", erro.getMessage());
	}
}
