package com.projeto.service;

import java.util.EnumSet;
import java.util.Set;

import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.Perfil;

public final class CatalogoFuncaoPadrao {

	private static final Set<AuthorityFuncao> ADMIN = EnumSet.of(
			AuthorityFuncao.CONTA_PROPRIA,
			AuthorityFuncao.RESTAURANTE_GERIR,
			AuthorityFuncao.EQUIPE_LER,
			AuthorityFuncao.EQUIPE_USUARIO_GERIR,
			AuthorityFuncao.EQUIPE_GESTOR_GERIR,
			AuthorityFuncao.EQUIPE_PERFIL_TROCAR,
			AuthorityFuncao.EQUIPE_SENHA_REDEFINIR,
			AuthorityFuncao.EQUIPE_FUNCAO_ATRIBUIR,
			AuthorityFuncao.CARDAPIO_LER,
			AuthorityFuncao.CARDAPIO_ESCREVER,
			AuthorityFuncao.CUSTO_LER,
			AuthorityFuncao.COMANDA_LER_PROPRIA,
			AuthorityFuncao.COMANDA_LER_UNIDADE,
			AuthorityFuncao.COMANDA_ABRIR_PROPRIA,
			AuthorityFuncao.COMANDA_ABRIR_UNIDADE,
			AuthorityFuncao.COMANDA_FECHAR,
			AuthorityFuncao.PEDIDO_LER_PROPRIO,
			AuthorityFuncao.PEDIDO_LER_UNIDADE,
			AuthorityFuncao.PEDIDO_LANCAR,
			AuthorityFuncao.PEDIDO_EDITAR,
			AuthorityFuncao.PEDIDO_CANCELAR,
			AuthorityFuncao.PEDIDO_ESTORNAR_ATIVO,
			AuthorityFuncao.PEDIDO_ESTORNAR_PAGO,
			AuthorityFuncao.ANALISE_VER_OPERACIONAL,
			AuthorityFuncao.ANALISE_VER_FINANCEIRO,
			AuthorityFuncao.REDE_VER,
			AuthorityFuncao.CATALOGO_FUNCOES_GERIR,
			AuthorityFuncao.RECLAMACAO_ABRIR,
			AuthorityFuncao.RECLAMACAO_LER,
			AuthorityFuncao.RECLAMACAO_RESPONDER,
			AuthorityFuncao.AUDITORIA_VER);

	private static final Set<AuthorityFuncao> GESTOR = EnumSet.copyOf(ADMIN);

	static {
		// TODO(migracao): o mapa pedia o pacote do admin sem RESTAURANTE_GERIR, REDE_VER e
		// EQUIPE_GESTOR_GERIR. EQUIPE_PERFIL_TROCAR e RECLAMACAO_RESPONDER também saem,
		// porque hoje o gestor não troca perfil nem responde reclamação.
		GESTOR.remove(AuthorityFuncao.RESTAURANTE_GERIR);
		GESTOR.remove(AuthorityFuncao.REDE_VER);
		GESTOR.remove(AuthorityFuncao.EQUIPE_GESTOR_GERIR);
		GESTOR.remove(AuthorityFuncao.EQUIPE_PERFIL_TROCAR);
		GESTOR.remove(AuthorityFuncao.RECLAMACAO_RESPONDER);
		GESTOR.remove(AuthorityFuncao.CATALOGO_FUNCOES_GERIR);
		GESTOR.remove(AuthorityFuncao.AUDITORIA_VER);
	}

	private CatalogoFuncaoPadrao() {
	}

	public static Set<AuthorityFuncao> doPerfil(Perfil perfil) {
		return switch (perfil) {
			case TI -> EnumSet.of(
					AuthorityFuncao.PLATAFORMA_TI_GERIR,
					AuthorityFuncao.PLATAFORMA_ADMIN_GERIR,
					// TODO(migracao): a TI responde reclamação de destino TI; o mapa escrito não listava isso.
					AuthorityFuncao.RECLAMACAO_LER,
					AuthorityFuncao.RECLAMACAO_RESPONDER);
			case ADMINISTRADOR -> EnumSet.copyOf(ADMIN);
			case GESTOR -> EnumSet.copyOf(GESTOR);
			case USUARIO -> EnumSet.of(
					AuthorityFuncao.CONTA_PROPRIA,
					AuthorityFuncao.COMANDA_LER_PROPRIA,
					AuthorityFuncao.COMANDA_ABRIR_PROPRIA,
					AuthorityFuncao.PEDIDO_LER_PROPRIO,
					AuthorityFuncao.PEDIDO_LANCAR,
					AuthorityFuncao.CARDAPIO_LER);
		};
	}

	public static boolean dePlataforma(AuthorityFuncao authority) {
		return authority == AuthorityFuncao.PLATAFORMA_TI_GERIR || authority == AuthorityFuncao.PLATAFORMA_ADMIN_GERIR;
	}
}
