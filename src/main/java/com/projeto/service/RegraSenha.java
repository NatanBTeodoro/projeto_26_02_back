package com.projeto.service;

public final class RegraSenha {

	public static final String MENSAGEM = "A senha precisa ter de 6 a 72 caracteres, com uma letra maiúscula, um número e um caractere especial";

	private RegraSenha() {
	}

	public static boolean aceita(String senha) {
		if (senha == null || senha.length() < 6 || senha.length() > 72) {
			return false;
		}
		boolean maiuscula = false;
		boolean numero = false;
		boolean especial = false;
		for (int indice = 0; indice < senha.length(); indice++) {
			char caractere = senha.charAt(indice);
			if (Character.isUpperCase(caractere)) {
				maiuscula = true;
			} else if (Character.isDigit(caractere)) {
				numero = true;
			} else if (!Character.isLetter(caractere)) {
				especial = true;
			}
		}
		return maiuscula && numero && especial;
	}
}
