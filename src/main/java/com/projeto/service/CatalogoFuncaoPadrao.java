package com.projeto.service;

import java.util.EnumSet;
import java.util.Set;

import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.Perfil;

public final class CatalogoFuncaoPadrao {

	private static final Set<AuthorityFuncao> ADMIN = EnumSet.of(
			AuthorityFuncao.EQUIPE_LER,
			AuthorityFuncao.EQUIPE_USUARIO_GERIR,
			AuthorityFuncao.EQUIPE_GESTOR_GERIR,
			AuthorityFuncao.EQUIPE_SENHA_REDEFINIR,
			AuthorityFuncao.EQUIPE_FUNCAO_ATRIBUIR,
			AuthorityFuncao.CATALOGO_FUNCOES_GERIR,
			AuthorityFuncao.CARDAPIO_LER,
			AuthorityFuncao.CARDAPIO_ESCREVER,
			AuthorityFuncao.CUSTO_LER,
			AuthorityFuncao.COMANDA_LER,
			AuthorityFuncao.COMANDA_ABRIR,
			AuthorityFuncao.COMANDA_FECHAR,
			AuthorityFuncao.PEDIDO_LER,
			AuthorityFuncao.PEDIDO_LANCAR,
			AuthorityFuncao.PEDIDO_EDITAR,
			AuthorityFuncao.PEDIDO_CANCELAR,
			AuthorityFuncao.PEDIDO_ESTORNAR,
			AuthorityFuncao.PEDIDO_ESTORNAR_PAGO,
			AuthorityFuncao.ANALISE_VER_OPERACIONAL,
			AuthorityFuncao.ANALISE_VER_FINANCEIRO,
			AuthorityFuncao.RECLAMACAO_ABRIR,
			AuthorityFuncao.RECLAMACAO_LER,
			AuthorityFuncao.RECLAMACAO_RESPONDER,
			AuthorityFuncao.AUDITORIA_VER);

	private static final Set<AuthorityFuncao> GESTOR = EnumSet.copyOf(ADMIN);

	static {
		GESTOR.remove(AuthorityFuncao.EQUIPE_GESTOR_GERIR);
		GESTOR.remove(AuthorityFuncao.CATALOGO_FUNCOES_GERIR);
		GESTOR.remove(AuthorityFuncao.AUDITORIA_VER);
	}

	private CatalogoFuncaoPadrao() {
	}

	public static Set<AuthorityFuncao> doPerfil(Perfil perfil) {
		return switch (perfil) {
			case TI -> EnumSet.of(
					AuthorityFuncao.PLATAFORMA_TI_GERIR,
					AuthorityFuncao.PLATAFORMA_ADMIN_GERIR);
			case ADMINISTRADOR -> EnumSet.copyOf(ADMIN);
			case GESTOR -> EnumSet.copyOf(GESTOR);
			case USUARIO -> EnumSet.of(
					AuthorityFuncao.COMANDA_LER,
					AuthorityFuncao.COMANDA_ABRIR,
					AuthorityFuncao.PEDIDO_LER,
					AuthorityFuncao.PEDIDO_LANCAR,
					AuthorityFuncao.CARDAPIO_LER);
		};
	}

	public static boolean dePlataforma(AuthorityFuncao authority) {
		return authority == AuthorityFuncao.PLATAFORMA_TI_GERIR || authority == AuthorityFuncao.PLATAFORMA_ADMIN_GERIR;
	}

	public static int tamanhoDoCatalogo() {
		return AuthorityFuncao.values().length;
	}
}
