package com.projeto.service;

import java.util.Locale;

import com.projeto.util.exception.RegraDeNegocioException;

public final class AnexoReclamacao {

	public static final int LIMITE_BYTES = 5 * 1024 * 1024;
	public static final String MENSAGEM_TIPO = "Envie uma imagem JPG, PNG, WEBP ou um PDF";
	public static final String MENSAGEM_TAMANHO = "O anexo passa de 5 MB";
	public static final String MENSAGEM_QUANTIDADE = "A reclamação aceita apenas um anexo";

	private AnexoReclamacao() {
	}

	public record Arquivo(String nome, String tipo) {
	}

	public static Arquivo validar(String nomeOriginal, byte[] conteudo) {
		if (conteudo == null || conteudo.length == 0) {
			throw new RegraDeNegocioException("Não foi possível ler o anexo");
		}
		if (conteudo.length > LIMITE_BYTES) {
			throw new RegraDeNegocioException(MENSAGEM_TAMANHO);
		}
		String tipo = tipoPorAssinatura(conteudo);
		String extensao = extensao(nomeOriginal);
		if (tipo == null || !combina(extensao, tipo)) {
			throw new RegraDeNegocioException(MENSAGEM_TIPO);
		}
		return new Arquivo(nomeSeguro(nomeOriginal, extensao), tipo);
	}

	public static Arquivo paraDownload(String nomeOriginal, byte[] conteudo) {
		if (conteudo == null || conteudo.length == 0) {
			return new Arquivo("anexo.bin", "application/octet-stream");
		}
		String tipo = tipoPorAssinatura(conteudo);
		String extensao = extensao(nomeOriginal);
		if (tipo == null || !combina(extensao, tipo)) {
			return new Arquivo("anexo.bin", "application/octet-stream");
		}
		return new Arquivo(nomeSeguro(nomeOriginal, extensao), tipo);
	}

	private static boolean combina(String extensao, String tipo) {
		return switch (tipo) {
			case "image/jpeg" -> extensao.equals("jpg") || extensao.equals("jpeg") || extensao.equals("jfif");
			case "image/png" -> extensao.equals("png");
			case "image/webp" -> extensao.equals("webp");
			case "application/pdf" -> extensao.equals("pdf");
			default -> false;
		};
	}

	private static String tipoPorAssinatura(byte[] conteudo) {
		if (comeca(conteudo, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF })) {
			return "image/jpeg";
		}
		if (comeca(conteudo, new byte[] {
				(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A })) {
			return "image/png";
		}
		if (conteudo.length >= 12
				&& comeca(conteudo, new byte[] { 0x52, 0x49, 0x46, 0x46 })
				&& conteudo[8] == 0x57
				&& conteudo[9] == 0x45
				&& conteudo[10] == 0x42
				&& conteudo[11] == 0x50) {
			return "image/webp";
		}
		if (comeca(conteudo, new byte[] { 0x25, 0x50, 0x44, 0x46, 0x2D })) {
			return "application/pdf";
		}
		return null;
	}

	private static boolean comeca(byte[] conteudo, byte[] assinatura) {
		if (conteudo.length < assinatura.length) {
			return false;
		}
		for (int indice = 0; indice < assinatura.length; indice++) {
			if (conteudo[indice] != assinatura[indice]) {
				return false;
			}
		}
		return true;
	}

	private static String extensao(String nome) {
		String arquivo = nomeArquivo(nome).toLowerCase(Locale.ROOT);
		int ponto = arquivo.lastIndexOf('.');
		if (ponto < 0 || ponto == arquivo.length() - 1) {
			return "";
		}
		return arquivo.substring(ponto + 1);
	}

	private static String nomeSeguro(String original, String extensao) {
		String base = nomeArquivo(original);
		if (base.length() > 255) {
			base = base.substring(base.length() - 255);
		}
		if (!base.toLowerCase(Locale.ROOT).endsWith("." + extensao)) {
			return "anexo." + extensao;
		}
		return base;
	}

	private static String nomeArquivo(String original) {
		String base = original == null ? "" : original.replace('\\', '/');
		int barra = base.lastIndexOf('/');
		if (barra >= 0) {
			base = base.substring(barra + 1);
		}
		StringBuilder limpo = new StringBuilder();
		for (int indice = 0; indice < base.length(); indice++) {
			char caractere = base.charAt(indice);
			if (caractere == '"' || caractere == '\r' || caractere == '\n' || caractere == '\0') {
				continue;
			}
			limpo.append(caractere);
		}
		return limpo.toString().trim();
	}
}
