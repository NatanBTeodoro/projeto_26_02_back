package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.projeto.util.exception.RegraDeNegocioException;

class AnexoReclamacaoTest {

	@Test
	void aceitaJpegPngWebpPdfEJfif() {
		assertEquals("image/jpeg", AnexoReclamacao.validar("foto.jpg", jpeg()).tipo());
		assertEquals("image/jpeg", AnexoReclamacao.validar("foto.jpeg", jpeg()).tipo());
		assertEquals("foto.jfif", AnexoReclamacao.validar("foto.jfif", jpeg()).nome());
		assertEquals("image/jpeg", AnexoReclamacao.validar("foto.jfif", jpeg()).tipo());
		assertEquals("image/png", AnexoReclamacao.validar("foto.png", png()).tipo());
		assertEquals("image/webp", AnexoReclamacao.validar("foto.webp", webp()).tipo());
		assertEquals("application/pdf", AnexoReclamacao.validar("print.pdf", pdf()).tipo());
	}

	@Test
	void recusaExtensaoForaDaListaMesmoComConteudoDeImagem() {
		RegraDeNegocioException erro = assertThrows(
				RegraDeNegocioException.class,
				() -> AnexoReclamacao.validar("foto.html", jpeg()));
		assertEquals(AnexoReclamacao.MENSAGEM_TIPO, erro.getMessage());
		assertThrows(RegraDeNegocioException.class, () -> AnexoReclamacao.validar("foto.svg", jpeg()));
		assertThrows(RegraDeNegocioException.class, () -> AnexoReclamacao.validar("foto.exe", jpeg()));
		assertThrows(RegraDeNegocioException.class, () -> AnexoReclamacao.validar("foto.zip", jpeg()));
	}

	@Test
	void recusaConteudoQueNaoBateComAExtensao() {
		assertThrows(RegraDeNegocioException.class, () -> AnexoReclamacao.validar("pagina.jpg", html()));
		assertThrows(RegraDeNegocioException.class, () -> AnexoReclamacao.validar("icone.png", svg()));
		assertThrows(RegraDeNegocioException.class, () -> AnexoReclamacao.validar("arquivo.pdf", zip()));
		assertThrows(RegraDeNegocioException.class, () -> AnexoReclamacao.validar("foto.png", jpeg()));
	}

	@Test
	void recusaArquivoMaiorQueCincoMb() {
		byte[] grande = new byte[AnexoReclamacao.LIMITE_BYTES + 1];
		grande[0] = (byte) 0xFF;
		grande[1] = (byte) 0xD8;
		grande[2] = (byte) 0xFF;
		RegraDeNegocioException erro = assertThrows(
				RegraDeNegocioException.class,
				() -> AnexoReclamacao.validar("foto.jpg", grande));
		assertEquals(AnexoReclamacao.MENSAGEM_TAMANHO, erro.getMessage());
	}

	@Test
	void downloadDeArquivoNaoPermitidoNaoMantemOTipoPerigoso() {
		AnexoReclamacao.Arquivo arquivo = AnexoReclamacao.paraDownload("pagina.html", html());
		assertEquals("anexo.bin", arquivo.nome());
		assertEquals("application/octet-stream", arquivo.tipo());
	}

	@Test
	void tiraOCaminhoDoNome() {
		assertEquals("foto.jpg", AnexoReclamacao.validar("../fotos/foto.jpg", jpeg()).nome());
	}

	private static byte[] jpeg() {
		return new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9 };
	}

	private static byte[] png() {
		return new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };
	}

	private static byte[] webp() {
		return new byte[] { 0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50 };
	}

	private static byte[] pdf() {
		return "%PDF-1.4".getBytes();
	}

	private static byte[] html() {
		return "<html><script>alert(1)</script></html>".getBytes();
	}

	private static byte[] svg() {
		return "<svg onload=\"alert(1)\"></svg>".getBytes();
	}

	private static byte[] zip() {
		return new byte[] { 0x50, 0x4B, 0x03, 0x04 };
	}
}
