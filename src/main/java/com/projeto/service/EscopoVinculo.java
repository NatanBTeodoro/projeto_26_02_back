package com.projeto.service;

import com.projeto.entity.enums.Perfil;

public final class EscopoVinculo {

	private EscopoVinculo() {
	}

	/** Gestor e administrador enxergam a casa. Usuário de salão enxerga a própria conta. */
	public static boolean deUnidade(Perfil perfil) {
		// vinculo, nao funcao
		return perfil == Perfil.GESTOR || perfil == Perfil.ADMINISTRADOR;
	}
}
