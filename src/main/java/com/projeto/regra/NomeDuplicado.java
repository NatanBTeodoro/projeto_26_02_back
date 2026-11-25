package com.projeto.regra;

import java.util.Collection;

public final class NomeDuplicado {

	private static final int LIMITE = 120;

	private NomeDuplicado() {
	}

	public static String reservar(String digitado, Collection<String> existentes) {
		String nome = digitado == null ? "" : digitado.trim();
		if (nome.isEmpty() || !ocupa(existentes, nome)) {
			return limitar(nome);
		}
		String base = nome.replaceFirst("(?i)#\\d+$", "");
		if (base.isEmpty()) {
			base = nome;
		}
		int numero = 2;
		String candidato = comSufixo(base, numero);
		while (ocupa(existentes, candidato)) {
			numero++;
			candidato = comSufixo(base, numero);
		}
		return candidato;
	}

	private static String comSufixo(String base, int numero) {
		String sufixo = "#" + numero;
		String raiz = base;
		if (raiz.length() + sufixo.length() > LIMITE) {
			raiz = raiz.substring(0, LIMITE - sufixo.length());
		}
		return raiz + sufixo;
	}

	private static boolean ocupa(Collection<String> existentes, String nome) {
		if (existentes == null) {
			return false;
		}
		for (String existente : existentes) {
			if (existente != null && existente.equalsIgnoreCase(nome)) {
				return true;
			}
		}
		return false;
	}

	private static String limitar(String nome) {
		if (nome.length() <= LIMITE) {
			return nome;
		}
		return nome.substring(0, LIMITE);
	}
}
