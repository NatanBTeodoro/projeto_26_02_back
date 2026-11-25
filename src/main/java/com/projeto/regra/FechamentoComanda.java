package com.projeto.regra;

import java.util.List;

import com.projeto.entity.Comanda;
import com.projeto.entity.Pedido;
import com.projeto.entity.enums.StatusPedido;

public final class FechamentoComanda {

	private FechamentoComanda() {
	}

	public static int aplicar(Comanda comanda, List<Pedido> pedidos) {
		comanda.setAberta(false);
		int pagos = 0;
		for (Pedido pedido : pedidos) {
			if (pedido.getStatus() != StatusPedido.ATIVO) {
				continue;
			}
			pedido.setStatus(StatusPedido.PAGO);
			pagos++;
		}
		return pagos;
	}
}
