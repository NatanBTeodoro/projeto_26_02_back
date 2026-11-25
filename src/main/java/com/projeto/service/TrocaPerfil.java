package com.projeto.service;

import org.springframework.security.access.AccessDeniedException;

import com.projeto.entity.enums.Perfil;
import com.projeto.util.exception.RegraDeNegocioException;

public final class TrocaPerfil {

	private TrocaPerfil() {
	}

	public static void validar(Perfil autor, Long autorId, Long alvoId, Perfil perfilAlvo, Perfil novo) {
		// A função EQUIPE_PERFIL_TROCAR é exigida em UsuarioService. O autor fica para o teste de vínculo.
		if (autor != Perfil.ADMINISTRADOR && autor != Perfil.GESTOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		if (alvoId != null && alvoId.equals(autorId)) {
			throw new RegraDeNegocioException("Você não pode alterar o próprio perfil");
		}
		if ((perfilAlvo != Perfil.GESTOR && perfilAlvo != Perfil.USUARIO)
				|| (novo != Perfil.GESTOR && novo != Perfil.USUARIO)) {
			throw new RegraDeNegocioException("Perfil inválido");
		}
	}
}
