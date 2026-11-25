package com.projeto.regra;

import org.springframework.security.access.AccessDeniedException;

import com.projeto.entity.enums.Perfil;
import com.projeto.util.exception.RegraDeNegocioException;

public final class TrocaPerfil {

	private TrocaPerfil() {
	}

	public static void validar(Perfil autor, Long autorId, Long alvoId, Perfil perfilAlvo, Perfil novo) {
		// vinculo, nao funcao: só o administrador troca perfil. Gestor e TI ficam de fora.
		if (autor != Perfil.ADMINISTRADOR) {
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
