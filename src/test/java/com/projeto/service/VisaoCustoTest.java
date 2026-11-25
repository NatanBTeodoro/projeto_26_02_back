package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.entity.Pedido;
import com.projeto.entity.PedidoItem;
import com.projeto.entity.Produto;
import com.projeto.entity.Restaurante;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.StatusPedido;
import com.projeto.repository.PedidoRepository;
import com.projeto.seguranca.EscopoOperacao;

@ExtendWith(MockitoExtension.class)
class VisaoCustoTest {

	@Mock
	private PedidoRepository pedidos;
	@Mock
	private EscopoOperacao escopo;
	@Mock
	private Autorizacao autorizacao;

	@BeforeEach
	void pedidosDoPeriodo() {
		Restaurante casa = new Restaurante();
		casa.setId(1L);
		when(escopo.restauranteDoContexto()).thenReturn(casa);
		when(pedidos.findByRestauranteIdAndDataHoraGreaterThanEqualAndDataHoraLessThan(any(), any(), any()))
				.thenReturn(List.of(pedido()));
	}

	@Test
	void soOperacionalRecebeCustoNulo() {
		when(autorizacao.tem(AuthorityFuncao.ANALISE_VER_OPERACIONAL)).thenReturn(true);
		when(autorizacao.tem(AuthorityFuncao.ANALISE_VER_FINANCEIRO)).thenReturn(false);

		var analise = new AnaliseService(pedidos, escopo, autorizacao)
				.consultar(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
		var painel = new DashboardService(pedidos, escopo, autorizacao)
				.consultar(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

		assertNull(analise.custoBruto());
		assertNull(analise.coberturaDeCusto());
		assertNull(painel.custoBruto());
		assertNull(painel.coberturaDeCusto());
	}

	@Test
	void financeiroRecebeOCustoBrutoDoPedido() {
		when(autorizacao.tem(AuthorityFuncao.ANALISE_VER_FINANCEIRO)).thenReturn(true);

		var analise = new AnaliseService(pedidos, escopo, autorizacao)
				.consultar(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

		assertEquals(new BigDecimal("3.00"), analise.custoBruto());
		assertEquals(new BigDecimal("50.00"), analise.coberturaDeCusto());
	}

	private static Pedido pedido() {
		Pedido pedido = new Pedido();
		pedido.setStatus(StatusPedido.PAGO);
		pedido.setDataHora(Instant.parse("2026-01-10T15:00:00Z"));
		pedido.getItens().add(item(pedido, 1L, "Agua", "1.50"));
		pedido.getItens().add(item(pedido, 2L, "Suco", null));
		return pedido;
	}

	private static PedidoItem item(Pedido pedido, long id, String nome, String custo) {
		Produto produto = new Produto();
		produto.setId(id);
		produto.setNome(nome);
		PedidoItem item = new PedidoItem();
		item.setPedido(pedido);
		item.setProduto(produto);
		item.setQuantidade(2);
		item.setPrecoUnitario(new BigDecimal("5.00"));
		item.setCustoUnitario(custo == null ? null : new BigDecimal(custo));
		return item;
	}
}
