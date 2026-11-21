package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.projeto.entity.Comanda;
import com.projeto.entity.Pedido;
import com.projeto.entity.enums.StatusPedido;

class FechamentoComandaTest {

	@Test
	void fecharMarcaSoOsPedidosAtivosComoPagos() {
		Comanda comanda = new Comanda();
		comanda.setAberta(true);
		Pedido aberto = new Pedido();
		aberto.setStatus(StatusPedido.ATIVO);
		Pedido cancelado = new Pedido();
		cancelado.setStatus(StatusPedido.CANCELADO);

		int pagos = FechamentoComanda.aplicar(comanda, List.of(aberto, cancelado));

		assertFalse(comanda.isAberta());
		assertEquals(1, pagos);
		assertEquals(StatusPedido.PAGO, aberto.getStatus());
		assertEquals(StatusPedido.CANCELADO, cancelado.getStatus());
	}
}
