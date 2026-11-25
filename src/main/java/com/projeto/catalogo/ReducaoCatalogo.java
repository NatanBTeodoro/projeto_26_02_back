package com.projeto.catalogo;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ReducaoCatalogo {

	private ReducaoCatalogo() {
	}

	public static boolean removida(String authority) {
		return "CONTA_PROPRIA".equals(authority)
				|| "RESTAURANTE_GERIR".equals(authority)
				|| "REDE_VER".equals(authority)
				|| "EQUIPE_PERFIL_TROCAR".equals(authority);
	}

	public static String consolidar(String authority) {
		return switch (authority) {
			case "COMANDA_LER_PROPRIA", "COMANDA_LER_UNIDADE" -> "COMANDA_LER";
			case "COMANDA_ABRIR_PROPRIA", "COMANDA_ABRIR_UNIDADE" -> "COMANDA_ABRIR";
			case "PEDIDO_LER_PROPRIO", "PEDIDO_LER_UNIDADE" -> "PEDIDO_LER";
			case "PEDIDO_ESTORNAR_ATIVO" -> "PEDIDO_ESTORNAR";
			default -> authority;
		};
	}

	public static Set<String> resultado(Set<String> atuais) {
		Set<String> saida = new LinkedHashSet<>();
		for (String authority : atuais) {
			if (removida(authority)) {
				continue;
			}
			saida.add(consolidar(authority));
		}
		return saida;
	}
}
