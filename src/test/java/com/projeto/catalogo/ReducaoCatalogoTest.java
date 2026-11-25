package com.projeto.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class ReducaoCatalogoTest {

	@Test
	void leituraDuplicadaViraUmaAuthorityEEstornoAtivoViraEstornar() {
		Set<String> depois = ReducaoCatalogo.resultado(Set.of(
				"COMANDA_LER_PROPRIA",
				"COMANDA_LER_UNIDADE",
				"PEDIDO_ESTORNAR_ATIVO",
				"PEDIDO_ESTORNAR_PAGO"));

		assertEquals(Set.of("COMANDA_LER", "PEDIDO_ESTORNAR", "PEDIDO_ESTORNAR_PAGO"), depois);
	}

	@Test
	void vinculoSomeDaAtribuicao() {
		Set<String> depois = ReducaoCatalogo.resultado(Set.of(
				"CONTA_PROPRIA",
				"EQUIPE_PERFIL_TROCAR",
				"RESTAURANTE_GERIR",
				"REDE_VER",
				"CARDAPIO_LER"));

		assertEquals(Set.of("CARDAPIO_LER"), depois);
		assertFalse(depois.contains("CONTA_PROPRIA"));
		assertFalse(depois.contains("EQUIPE_PERFIL_TROCAR"));
		assertTrue(ReducaoCatalogo.removida("REDE_VER"));
		assertTrue(ReducaoCatalogo.removida("RESTAURANTE_GERIR"));
	}
}
