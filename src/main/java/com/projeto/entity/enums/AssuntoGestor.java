package com.projeto.entity.enums;

import com.projeto.util.exception.RegraDeNegocioException;

public enum AssuntoGestor {
	PROBLEMA_NO_SISTEMA("Problema no Sistema", DestinoReclamacao.TI),
	CARDAPIO("Cardápio", DestinoReclamacao.ADMINISTRADOR),
	PEDIDOS("Pedidos", DestinoReclamacao.ADMINISTRADOR),
	EQUIPE("Equipe", DestinoReclamacao.ADMINISTRADOR),
	OUTRO("Outro", DestinoReclamacao.ADMINISTRADOR);

	private final String rotulo;
	private final DestinoReclamacao destino;

	AssuntoGestor(String rotulo, DestinoReclamacao destino) {
		this.rotulo = rotulo;
		this.destino = destino;
	}

	public String rotulo() {
		return rotulo;
	}

	public DestinoReclamacao destino() {
		return destino;
	}

	public static AssuntoGestor exigir(String codigo) {
		if (codigo == null || codigo.isBlank()) {
			throw new RegraDeNegocioException("Escolha o assunto");
		}
		try {
			return AssuntoGestor.valueOf(codigo.trim());
		} catch (IllegalArgumentException ex) {
			throw new RegraDeNegocioException("Escolha um assunto válido");
		}
	}
}
