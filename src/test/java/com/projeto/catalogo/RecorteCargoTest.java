package com.projeto.catalogo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.Perfil;

class RecorteCargoTest {

	@Test
	void usuarioSoRecebeAtendenteCaixaEEstoquista() {
		assertTrue(RecorteCargo.cargoPermitido(Perfil.USUARIO, "Atendente"));
		assertTrue(RecorteCargo.cargoPermitido(Perfil.USUARIO, "Caixa"));
		assertTrue(RecorteCargo.cargoPermitido(Perfil.USUARIO, "Estoquista"));
		assertFalse(RecorteCargo.cargoPermitido(Perfil.USUARIO, "Gerente"));
		assertFalse(RecorteCargo.cargoPermitido(Perfil.USUARIO, "Supervisor"));
		assertFalse(RecorteCargo.cargoPermitido(Perfil.USUARIO, "Analista"));
	}

	@Test
	void gestorNaoRecebeOsCargosDeUsuario() {
		assertFalse(RecorteCargo.cargoPermitido(Perfil.GESTOR, "Atendente"));
		assertFalse(RecorteCargo.cargoPermitido(Perfil.GESTOR, "Caixa"));
		assertFalse(RecorteCargo.cargoPermitido(Perfil.GESTOR, "Estoquista"));
		assertTrue(RecorteCargo.cargoPermitido(Perfil.GESTOR, "Gerente"));
		assertTrue(RecorteCargo.cargoPermitido(Perfil.GESTOR, "Supervisor"));
		assertTrue(RecorteCargo.cargoPermitido(Perfil.GESTOR, "Analista"));
	}

	@Test
	void funcoesSeguemOMesmoRecorte() {
		assertTrue(RecorteCargo.funcaoPermitida(Perfil.USUARIO, AuthorityFuncao.COMANDA_FECHAR));
		assertTrue(RecorteCargo.funcaoPermitida(Perfil.USUARIO, AuthorityFuncao.CARDAPIO_ESCREVER));
		assertFalse(RecorteCargo.funcaoPermitida(Perfil.USUARIO, AuthorityFuncao.ANALISE_VER_FINANCEIRO));
		assertTrue(RecorteCargo.funcaoPermitida(Perfil.GESTOR, AuthorityFuncao.ANALISE_VER_OPERACIONAL));
		assertFalse(RecorteCargo.funcaoPermitida(Perfil.GESTOR, AuthorityFuncao.AUDITORIA_VER));
	}
}
