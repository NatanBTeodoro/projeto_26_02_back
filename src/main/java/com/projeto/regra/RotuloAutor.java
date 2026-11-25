package com.projeto.regra;

import com.projeto.entity.enums.Perfil;

public final class RotuloAutor {

	private static final int LIMITE = 120;

	private RotuloAutor() {
	}

	public static String direto(Perfil perfil, String nome, String email) {
		// vinculo, nao funcao: só o texto da assinatura.
		if (perfil == Perfil.ADMINISTRADOR) {
			return "Admin";
		}
		return assinatura(perfil, nome, email);
	}

	public static String noPedido(Perfil perfil, String nome, String email) {
		if (perfil != Perfil.GESTOR) {
			return null;
		}
		return assinatura(perfil, nome, email);
	}

	public static String assinatura(Perfil perfil, String nome, String email) {
		String base = nome != null && !nome.isBlank() ? nome.trim() : email == null ? "" : email.trim();
		if (perfil == Perfil.GESTOR) {
			return limitar(base + " · Gestor");
		}
		return limitar(base);
	}

	private static String limitar(String texto) {
		if (texto.length() <= LIMITE) {
			return texto;
		}
		return texto.substring(0, LIMITE);
	}
}
