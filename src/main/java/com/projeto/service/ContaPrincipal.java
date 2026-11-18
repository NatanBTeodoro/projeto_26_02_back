package com.projeto.service;

import java.util.Locale;

import com.projeto.entity.enums.Perfil;
import com.projeto.util.exception.RegraDeNegocioException;

public final class ContaPrincipal {

	static final String EMAIL = "admin@admin.com";

	private ContaPrincipal() {
	}

	public static boolean eh(String email, String configurado) {
		if (email == null || email.isBlank()) {
			return false;
		}
		String normalizado = email.trim().toLowerCase(Locale.ROOT);
		if (normalizado.equals(EMAIL)) {
			return true;
		}
		return configurado != null && normalizado.equals(configurado.trim().toLowerCase(Locale.ROOT));
	}

	public static void exigirPodeRemover(String email, String configurado) {
		if (eh(email, configurado)) {
			throw new RegraDeNegocioException("A conta principal não pode ser removida");
		}
	}

	public static void exigirPodeRebaixar(String email, String configurado, Perfil novo) {
		if (eh(email, configurado) && novo != null && novo != Perfil.TI) {
			throw new RegraDeNegocioException("A conta principal não pode ser rebaixada");
		}
	}
}
