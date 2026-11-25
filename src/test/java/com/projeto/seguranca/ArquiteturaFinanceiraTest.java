package com.projeto.seguranca;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Equivalente ao ArchUnit pedido na Fase 5, sem dependência nova.
 * Uma exceção justificada leva {@code @SuppressWarnings("arquitetura-financeira")}
 * no método e um comentário dizendo por quê.
 */
class ArquiteturaFinanceiraTest {

	@Test
	void filtroFinanceiroExigeAuthorityENaoVoltaParaCargo() throws IOException {
		String fonte = Files.readString(Path.of("src/main/java/com/projeto/config/SecurityConfig.java"));
		assertTrue(fonte.contains("hasAnyAuthority(\"FUNCAO_COMANDA_FECHAR\")"));
		assertTrue(fonte.contains("hasAnyAuthority(\"FUNCAO_PEDIDO_ESTORNAR\", \"FUNCAO_PEDIDO_ESTORNAR_PAGO\")"));
		assertTrue(fonte.contains("hasAnyAuthority(\"FUNCAO_PEDIDO_CANCELAR\")"));
		assertFalse(trecho(fonte, "comandas/*/fechar").contains("hasAnyRole"));
		assertFalse(trecho(fonte, "pedidos/*/estorno").contains("hasAnyRole"));
		assertFalse(trecho(fonte, "pedidos/*/cancelamento").contains("hasAnyRole"));
		assertFalse(fonte.contains("CONTA_PROPRIA"));
		assertFalse(fonte.contains("RESTAURANTE_GERIR"));
		assertFalse(fonte.contains("REDE_VER"));
		assertFalse(fonte.contains("EQUIPE_PERFIL_TROCAR"));
	}

	@Test
	void operacaoFinanceiraExigeAuthorityAntesDeAcontecer() throws IOException {
		List<String> falhas = new ArrayList<>();
		conferir(falhas, "src/main/java/com/projeto/service/ComandaService.java", "fechar",
				"exigir(AuthorityFuncao.COMANDA_FECHAR)", "FechamentoComanda.aplicar");
		conferir(falhas, "src/main/java/com/projeto/service/PedidoService.java", "estornar",
				"exigir(AuthorityFuncao.PEDIDO_ESTORNAR", "setStatus(StatusPedido.ESTORNADO)");
		conferir(falhas, "src/main/java/com/projeto/service/PedidoService.java", "cancelar",
				"exigir(AuthorityFuncao.PEDIDO_CANCELAR)", "setStatus(StatusPedido.CANCELADO)");
		assertTrue(falhas.isEmpty(), String.join("\n", falhas));
	}

	private static void conferir(List<String> falhas, String arquivo, String metodo, String exigencia, String efeito)
			throws IOException {
		String fonte = Files.readString(Path.of(arquivo));
		String corpo = corpoDoMetodo(fonte, metodo);
		if (corpo.contains("@SuppressWarnings(\"arquitetura-financeira\")")) {
			return;
		}
		int trava = corpo.indexOf(exigencia);
		int operacao = corpo.indexOf(efeito);
		if (trava < 0 || operacao < 0 || trava > operacao) {
			falhas.add(arquivo + "#" + metodo + " chama a operação financeira sem a authority antes");
		}
	}

	private static String trecho(String fonte, String marcador) {
		int indice = fonte.indexOf(marcador);
		if (indice < 0) {
			return "";
		}
		int fim = fonte.indexOf('\n', indice);
		return fonte.substring(indice, fim < 0 ? fonte.length() : fim);
	}

	private static String corpoDoMetodo(String fonte, String metodo) {
		int assinatura = fonte.indexOf(" " + metodo + "(");
		if (assinatura < 0) {
			return "";
		}
		int abre = fonte.indexOf('{', assinatura);
		int profundidade = 0;
		for (int i = abre; i < fonte.length(); i++) {
			char caractere = fonte.charAt(i);
			if (caractere == '{') {
				profundidade++;
			} else if (caractere == '}') {
				profundidade--;
				if (profundidade == 0) {
					return fonte.substring(assinatura, i + 1);
				}
			}
		}
		return "";
	}
}
