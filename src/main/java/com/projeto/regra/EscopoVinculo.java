package com.projeto.regra;

import com.projeto.entity.enums.Perfil;

public final class EscopoVinculo {

	private EscopoVinculo() {
	}

	public static boolean deUnidade(Perfil perfil) {
		return perfil == Perfil.GESTOR || perfil == Perfil.ADMINISTRADOR;
	}
}
