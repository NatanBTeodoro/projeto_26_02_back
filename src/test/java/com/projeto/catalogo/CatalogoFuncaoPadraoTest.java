package com.projeto.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.Perfil;

class CatalogoFuncaoPadraoTest {

	@Test
	void cadaPerfilRecebeOPacoteReduzido() {
		var ti = CatalogoFuncaoPadrao.doPerfil(Perfil.TI);
		assertEquals(2, ti.size());
		assertTrue(ti.contains(AuthorityFuncao.PLATAFORMA_TI_GERIR));
		assertTrue(ti.contains(AuthorityFuncao.PLATAFORMA_ADMIN_GERIR));
		assertFalse(ti.contains(AuthorityFuncao.RECLAMACAO_LER));

		var admin = CatalogoFuncaoPadrao.doPerfil(Perfil.ADMINISTRADOR);
		assertEquals(24, admin.size());
		assertTrue(admin.contains(AuthorityFuncao.EQUIPE_GESTOR_GERIR));
		assertTrue(admin.contains(AuthorityFuncao.CATALOGO_FUNCOES_GERIR));
		assertTrue(admin.contains(AuthorityFuncao.AUDITORIA_VER));
		assertTrue(admin.contains(AuthorityFuncao.COMANDA_LER));
		assertTrue(admin.contains(AuthorityFuncao.PEDIDO_ESTORNAR));
		assertFalse(admin.contains(AuthorityFuncao.PLATAFORMA_TI_GERIR));

		var gestor = CatalogoFuncaoPadrao.doPerfil(Perfil.GESTOR);
		assertEquals(21, gestor.size());
		assertTrue(gestor.contains(AuthorityFuncao.EQUIPE_USUARIO_GERIR));
		assertTrue(gestor.contains(AuthorityFuncao.EQUIPE_FUNCAO_ATRIBUIR));
		assertTrue(gestor.contains(AuthorityFuncao.PEDIDO_ESTORNAR_PAGO));
		assertTrue(gestor.contains(AuthorityFuncao.RECLAMACAO_RESPONDER));
		assertFalse(gestor.contains(AuthorityFuncao.EQUIPE_GESTOR_GERIR));
		assertFalse(gestor.contains(AuthorityFuncao.CATALOGO_FUNCOES_GERIR));
		assertFalse(gestor.contains(AuthorityFuncao.AUDITORIA_VER));

		var usuario = CatalogoFuncaoPadrao.doPerfil(Perfil.USUARIO);
		assertEquals(5, usuario.size());
		assertTrue(usuario.contains(AuthorityFuncao.COMANDA_LER));
		assertTrue(usuario.contains(AuthorityFuncao.COMANDA_ABRIR));
		assertTrue(usuario.contains(AuthorityFuncao.PEDIDO_LER));
		assertTrue(usuario.contains(AuthorityFuncao.PEDIDO_LANCAR));
		assertTrue(usuario.contains(AuthorityFuncao.CARDAPIO_LER));
		assertFalse(usuario.contains(AuthorityFuncao.COMANDA_FECHAR));
		assertFalse(usuario.contains(AuthorityFuncao.CUSTO_LER));
		assertFalse(usuario.contains(AuthorityFuncao.ANALISE_VER_FINANCEIRO));
	}

	@Test
	void catalogoAtribuivelTemVinteESeisAuthorities() {
		assertEquals(26, CatalogoFuncaoPadrao.tamanhoDoCatalogo());
		for (AuthorityFuncao authority : AuthorityFuncao.values()) {
			String nome = authority.name();
			assertFalse(ReducaoCatalogo.removida(nome), nome);
		}
	}
}
