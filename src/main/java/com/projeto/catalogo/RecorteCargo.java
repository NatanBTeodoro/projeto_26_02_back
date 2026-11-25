package com.projeto.catalogo;

import java.util.EnumSet;
import java.util.Set;

import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.Perfil;

public final class RecorteCargo {

	public static final Set<String> DO_USUARIO = Set.of("Atendente", "Caixa", "Estoquista");

	private static final Set<AuthorityFuncao> FUNCOES_USUARIO = EnumSet.of(
			AuthorityFuncao.COMANDA_LER,
			AuthorityFuncao.COMANDA_ABRIR,
			AuthorityFuncao.COMANDA_FECHAR,
			AuthorityFuncao.PEDIDO_LER,
			AuthorityFuncao.PEDIDO_LANCAR,
			AuthorityFuncao.PEDIDO_EDITAR,
			AuthorityFuncao.PEDIDO_CANCELAR,
			AuthorityFuncao.PEDIDO_ESTORNAR,
			AuthorityFuncao.CARDAPIO_LER,
			AuthorityFuncao.CARDAPIO_ESCREVER,
			AuthorityFuncao.CUSTO_LER);

	private RecorteCargo() {
	}

	public static boolean cargoPermitido(Perfil perfil, String nome) {
		if (perfil == null || nome == null || nome.isBlank()) {
			return false;
		}
		if (perfil == Perfil.USUARIO) {
			return DO_USUARIO.contains(nome);
		}
		if (perfil == Perfil.GESTOR) {
			return !DO_USUARIO.contains(nome);
		}
		return false;
	}

	public static boolean funcaoPermitida(Perfil perfil, AuthorityFuncao authority) {
		if (perfil == null || authority == null) {
			return false;
		}
		if (perfil == Perfil.USUARIO) {
			return FUNCOES_USUARIO.contains(authority);
		}
		if (perfil == Perfil.GESTOR) {
			return CatalogoFuncaoPadrao.doPerfil(Perfil.GESTOR).contains(authority);
		}
		return false;
	}
}
