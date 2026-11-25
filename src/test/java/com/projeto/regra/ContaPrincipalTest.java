package com.projeto.regra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.projeto.entity.enums.Perfil;
import com.projeto.util.exception.RegraDeNegocioException;

class ContaPrincipalTest {

	@Test
	void reconheceOEmailPrincipalEConfigurado() {
		assertTrue(ContaPrincipal.eh("Admin@Admin.com", "outro@ti.com"));
		assertTrue(ContaPrincipal.eh("equipe@ti.com", "Equipe@ti.com"));
		assertFalse(ContaPrincipal.eh("gestor@loja.com", "admin@admin.com"));
	}

	@Test
	void principalNaoPodeSerRemovidaNemRebaixada() {
		RegraDeNegocioException remocao = assertThrows(
				RegraDeNegocioException.class,
				() -> ContaPrincipal.exigirPodeRemover("admin@admin.com", "admin@admin.com"));
		assertEquals("A conta principal não pode ser removida", remocao.getMessage());

		RegraDeNegocioException rebaixamento = assertThrows(
				RegraDeNegocioException.class,
				() -> ContaPrincipal.exigirPodeRebaixar("admin@admin.com", "admin@admin.com", Perfil.GESTOR));
		assertEquals("A conta principal não pode ser rebaixada", rebaixamento.getMessage());
	}

	@Test
	void outraContaPodeSerRemovidaEMantidaComoTi() {
		ContaPrincipal.exigirPodeRemover("nova@ti.com", "admin@admin.com");
		ContaPrincipal.exigirPodeRebaixar("admin@admin.com", "admin@admin.com", Perfil.TI);
	}
}
