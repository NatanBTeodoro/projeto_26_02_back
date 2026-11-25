package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.Perfil;

class CatalogoFuncaoPadraoTest {

	@Test
	void cadaPerfilRecebeOPacoteDaFaseZero() {
		var ti = CatalogoFuncaoPadrao.doPerfil(Perfil.TI);
		assertTrue(ti.contains(AuthorityFuncao.PLATAFORMA_TI_GERIR));
		assertTrue(ti.contains(AuthorityFuncao.PLATAFORMA_ADMIN_GERIR));
		assertFalse(ti.contains(AuthorityFuncao.CONTA_PROPRIA));
		assertFalse(ti.contains(AuthorityFuncao.CARDAPIO_ESCREVER));

		var admin = CatalogoFuncaoPadrao.doPerfil(Perfil.ADMINISTRADOR);
		assertTrue(admin.contains(AuthorityFuncao.RESTAURANTE_GERIR));
		assertTrue(admin.contains(AuthorityFuncao.EQUIPE_GESTOR_GERIR));
		assertTrue(admin.contains(AuthorityFuncao.REDE_VER));
		assertTrue(admin.contains(AuthorityFuncao.CATALOGO_FUNCOES_GERIR));
		assertTrue(admin.contains(AuthorityFuncao.AUDITORIA_VER));
		assertFalse(admin.contains(AuthorityFuncao.PLATAFORMA_TI_GERIR));

		var gestor = CatalogoFuncaoPadrao.doPerfil(Perfil.GESTOR);
		assertTrue(gestor.contains(AuthorityFuncao.EQUIPE_USUARIO_GERIR));
		assertTrue(gestor.contains(AuthorityFuncao.CARDAPIO_ESCREVER));
		assertTrue(gestor.contains(AuthorityFuncao.PEDIDO_ESTORNAR_PAGO));
		assertTrue(gestor.contains(AuthorityFuncao.EQUIPE_FUNCAO_ATRIBUIR));
		assertFalse(gestor.contains(AuthorityFuncao.RESTAURANTE_GERIR));
		assertFalse(gestor.contains(AuthorityFuncao.REDE_VER));
		assertFalse(gestor.contains(AuthorityFuncao.EQUIPE_GESTOR_GERIR));
		assertFalse(gestor.contains(AuthorityFuncao.EQUIPE_PERFIL_TROCAR));
		assertFalse(gestor.contains(AuthorityFuncao.RECLAMACAO_RESPONDER));

		var usuario = CatalogoFuncaoPadrao.doPerfil(Perfil.USUARIO);
		assertTrue(usuario.contains(AuthorityFuncao.PEDIDO_LANCAR));
		assertTrue(usuario.contains(AuthorityFuncao.CARDAPIO_LER));
		assertFalse(usuario.contains(AuthorityFuncao.CARDAPIO_ESCREVER));
		assertFalse(usuario.contains(AuthorityFuncao.CUSTO_LER));
		assertFalse(usuario.contains(AuthorityFuncao.ANALISE_VER_FINANCEIRO));
	}
}
