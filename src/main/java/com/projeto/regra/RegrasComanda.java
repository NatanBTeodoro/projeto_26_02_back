package com.projeto.regra;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.projeto.util.exception.RegraDeNegocioException;

public final class RegrasComanda {

	private RegrasComanda() {
	}

	public record Ocupacao(String nome, Integer mesa) {
	}

	public static void exigirMesmaMesa(Integer mesaAtual, Integer mesaNova) {
		if (mesaAtual != null && !mesaAtual.equals(mesaNova)) {
			throw new RegraDeNegocioException("A comanda já está na mesa " + mesaAtual);
		}
	}

	public static Set<String> nomesEmMaisDeUmaMesa(List<Ocupacao> abertas) {
		Map<String, Set<Integer>> mesas = new HashMap<>();
		if (abertas != null) {
			for (Ocupacao ocupacao : abertas) {
				if (ocupacao.nome() == null || ocupacao.nome().isBlank() || ocupacao.mesa() == null) {
					continue;
				}
				mesas.computeIfAbsent(chave(ocupacao.nome()), chave -> new HashSet<>()).add(ocupacao.mesa());
			}
		}
		Set<String> colisao = new HashSet<>();
		for (Map.Entry<String, Set<Integer>> entrada : mesas.entrySet()) {
			if (entrada.getValue().size() > 1) {
				colisao.add(entrada.getKey());
			}
		}
		return colisao;
	}

	public static boolean colide(String nome, Set<String> nomesEmMaisDeUmaMesa) {
		if (nome == null || nome.isBlank() || nomesEmMaisDeUmaMesa == null) {
			return false;
		}
		return nomesEmMaisDeUmaMesa.contains(chave(nome));
	}

	public static String rotulo(String nome, Integer mesa, boolean distinguirPorMesa) {
		if (nome == null || nome.isBlank() || mesa == null || !distinguirPorMesa) {
			return nome;
		}
		return nome.trim() + " · mesa " + mesa;
	}

	private static String chave(String nome) {
		return nome.trim().toLowerCase(Locale.ROOT);
	}
}
