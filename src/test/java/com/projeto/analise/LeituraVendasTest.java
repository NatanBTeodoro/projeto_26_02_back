package com.projeto.analise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.projeto.entity.Pedido;
import com.projeto.entity.PedidoItem;
import com.projeto.entity.Produto;
import com.projeto.entity.enums.StatusPedido;

class LeituraVendasTest {

	@Test
	void pedidoEstornadoNaoEntraNaQuantidadeNemNaReceita() {
		Pedido pago = pedido(StatusPedido.PAGO, 2, "10.00");
		Pedido aberto = pedido(StatusPedido.ATIVO, 4, "15.00");
		Pedido estornado = pedido(StatusPedido.ESTORNADO, 5, "30.00");

		List<ClassificadorCardapio.Linha> linhas = LeituraVendas.linhasAtivas(List.of(pago, aberto, estornado));

		assertEquals(1, LeituraVendas.contarPedidosAtivos(List.of(pago, aberto, estornado)));
		assertEquals(1, linhas.size());
		assertEquals(2, linhas.get(0).quantidade());
		assertEquals(new BigDecimal("20.00"), ClassificadorCardapio.receita(linhas));
	}

	@Test
	void custoBrutoUsaSoOItemQueTemCustoGravado() {
		List<ClassificadorCardapio.Linha> linhas = List.of(
				new ClassificadorCardapio.Linha(1L, "Agua", 2, new BigDecimal("5.00"), new BigDecimal("1.50"), "Bebidas"),
				new ClassificadorCardapio.Linha(2L, "Suco", 2, new BigDecimal("5.00"), null, "Bebidas"));

		LeituraVendas.Resumo resumo = LeituraVendas.resumir(linhas);

		assertEquals(new BigDecimal("20.00"), resumo.faturamentoTotal());
		assertEquals(new BigDecimal("10.00"), resumo.faturamentoComCusto());
		assertEquals(new BigDecimal("3.00"), resumo.custoBruto());
		assertEquals(new BigDecimal("7.00"), resumo.lucroBruto());
		assertEquals(new BigDecimal("50.00"), resumo.coberturaDeCusto());
	}

	private static Pedido pedido(StatusPedido status, int quantidade, String preco) {
		Produto produto = new Produto();
		produto.setId(1L);
		produto.setNome("Prato");
		Pedido pedido = new Pedido();
		pedido.setStatus(status);
		pedido.setDataHora(Instant.parse("2026-01-10T15:00:00Z"));
		PedidoItem item = new PedidoItem();
		item.setPedido(pedido);
		item.setProduto(produto);
		item.setQuantidade(quantidade);
		item.setPrecoUnitario(new BigDecimal(preco));
		item.setCustoUnitario(new BigDecimal("4.00"));
		pedido.getItens().add(item);
		return pedido;
	}
}
