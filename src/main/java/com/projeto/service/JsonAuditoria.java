package com.projeto.service;

import java.util.List;
import java.util.Map;

final class JsonAuditoria {

	private JsonAuditoria() {
	}

	static String escrever(Object valor) {
		StringBuilder texto = new StringBuilder();
		anexar(texto, valor);
		return texto.toString();
	}

	private static void anexar(StringBuilder texto, Object valor) {
		if (valor == null) {
			texto.append("null");
			return;
		}
		if (valor instanceof String textoValor) {
			texto.append('"');
			for (int i = 0; i < textoValor.length(); i++) {
				char caractere = textoValor.charAt(i);
				if (caractere == '"' || caractere == '\\') {
					texto.append('\\');
				}
				texto.append(caractere);
			}
			texto.append('"');
			return;
		}
		if (valor instanceof Number || valor instanceof Boolean) {
			texto.append(valor);
			return;
		}
		if (valor instanceof Map<?, ?> mapa) {
			texto.append('{');
			boolean primeiro = true;
			for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
				if (!primeiro) {
					texto.append(',');
				}
				primeiro = false;
				anexar(texto, String.valueOf(entrada.getKey()));
				texto.append(':');
				anexar(texto, entrada.getValue());
			}
			texto.append('}');
			return;
		}
		if (valor instanceof List<?> lista) {
			texto.append('[');
			boolean primeiro = true;
			for (Object item : lista) {
				if (!primeiro) {
					texto.append(',');
				}
				primeiro = false;
				anexar(texto, item);
			}
			texto.append(']');
			return;
		}
		anexar(texto, String.valueOf(valor));
	}
}
