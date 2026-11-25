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

	public record Aberta(Long id, String nome, Integer mesa) {
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

	public static Long reaproveitar(String nome, Integer mesa, List<Aberta> abertas) {
		if (nome == null || nome.isBlank() || mesa == null || abertas == null) {
			return null;
		}
		String alvo = chave(nome);
		Long mesmaMesa = null;
		Long semMesa = null;
		for (Aberta aberta : abertas) {
			if (aberta == null || aberta.id() == null || aberta.nome() == null || !chave(aberta.nome()).equals(alvo)) {
				continue;
			}
			if (aberta.mesa() != null && aberta.mesa().equals(mesa) && (mesmaMesa == null || aberta.id() < mesmaMesa)) {
				mesmaMesa = aberta.id();
			}
			if (aberta.mesa() == null && (semMesa == null || aberta.id() < semMesa)) {
				semMesa = aberta.id();
			}
		}
		return mesmaMesa != null ? mesmaMesa : semMesa;
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
