package com.projeto.service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.projeto.entity.enums.AuthorityFuncao;

public final class CargosPadrao {

	private CargosPadrao() {
	}

	public record Definicao(String nome, String descricao, Set<AuthorityFuncao> authorities) {
	}

	public static List<Definicao> todos() {
		Set<AuthorityFuncao> caixa = EnumSet.of(
				AuthorityFuncao.COMANDA_LER,
				AuthorityFuncao.COMANDA_ABRIR,
				AuthorityFuncao.COMANDA_FECHAR,
				AuthorityFuncao.PEDIDO_LER,
				AuthorityFuncao.PEDIDO_LANCAR,
				AuthorityFuncao.PEDIDO_EDITAR,
				AuthorityFuncao.PEDIDO_CANCELAR,
				AuthorityFuncao.PEDIDO_ESTORNAR);
		Set<AuthorityFuncao> supervisor = EnumSet.copyOf(caixa);
		supervisor.add(AuthorityFuncao.PEDIDO_ESTORNAR_PAGO);
		supervisor.add(AuthorityFuncao.ANALISE_VER_OPERACIONAL);
		return List.of(
				new Definicao(
						"Gerente",
						"Operação da casa, no mesmo recorte do gestor.",
						CatalogoFuncaoPadrao.doPerfil(com.projeto.entity.enums.Perfil.GESTOR)),
				new Definicao(
						"Caixa",
						"Fecha comanda, cancela e estorna pedido ativo de qualquer usuário da casa. Não estorna pedido pago.",
						caixa),
				new Definicao(
						"Atendente",
						"Lê e abre a própria comanda e lança o próprio pedido.",
						CatalogoFuncaoPadrao.doPerfil(com.projeto.entity.enums.Perfil.USUARIO)),
				new Definicao(
						"Estoquista",
						"Lê e edita o cardápio, inclusive o custo.",
						EnumSet.of(AuthorityFuncao.CARDAPIO_LER, AuthorityFuncao.CARDAPIO_ESCREVER, AuthorityFuncao.CUSTO_LER)),
				new Definicao(
						"Supervisor",
						"O caixa, mais o estorno de pedido pago e a análise operacional.",
						supervisor),
				new Definicao(
						"Analista",
						"Vê a análise operacional e a financeira, e lê cardápio e custo.",
						EnumSet.of(
								AuthorityFuncao.ANALISE_VER_OPERACIONAL,
								AuthorityFuncao.ANALISE_VER_FINANCEIRO,
								AuthorityFuncao.CARDAPIO_LER,
								AuthorityFuncao.CUSTO_LER)));
	}

	public static Definicao porNome(String nome) {
		return todos().stream()
				.filter(definicao -> definicao.nome().equals(nome))
				.findFirst()
				.orElseThrow();
	}
}
