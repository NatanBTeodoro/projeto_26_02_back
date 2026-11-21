package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.projeto.dto.PainelAdminDTO;
import com.projeto.entity.Categoria;
import com.projeto.entity.Pedido;
import com.projeto.entity.PedidoItem;
import com.projeto.entity.Produto;
import com.projeto.entity.Restaurante;
import com.projeto.entity.enums.StatusPedido;

class PainelAdminTest {

	private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
	private static final LocalDate DE = LocalDate.of(2026, 10, 23);
	private static final LocalDate ATE = LocalDate.of(2026, 11, 21);

	@Test
	void itemSemCustoEntraNoFaturamentoEFicaForaDoLucro() {
		Restaurante casa = restaurante(1L, "Casa");
		Produto comCusto = produto(1L, "Risoto");
		Produto semCusto = produto(2L, "Suco");
		List<Pedido> pedidos = List.of(
				pedido(casa, DE, StatusPedido.PAGO, comCusto, 1, "100.00", "40.00"),
				pedido(casa, DE, StatusPedido.PAGO, semCusto, 1, "50.00", null),
				pedido(casa, DE, StatusPedido.CANCELADO, comCusto, 1, "80.00", "10.00"));

		PainelAdminDTO.Response painel = PainelAdmin.montar(
				List.of(casa), pedidos, List.of(), pedidos, List.of(),
				true, DE, ATE, null, null);

		assertEquals(new BigDecimal("150.00"), kpi(painel, "faturamento").valor());
		assertEquals(new BigDecimal("60.00"), kpi(painel, "lucro").valor());
		assertEquals(new BigDecimal("60.00"), kpi(painel, "margem").valor());
		assertEquals("Margem calculada sobre 67% do faturamento", kpi(painel, "margem").complemento());
		assertEquals(new BigDecimal("2"), kpi(painel, "pedidos").valor());
		assertEquals(new BigDecimal("75.00"), kpi(painel, "ticket").valor());
		assertEquals("Sem período anterior", kpi(painel, "faturamento").variacao().texto());
		assertEquals("1 de 1 ativos", kpi(painel, "restaurantes").texto());
	}

	@Test
	void ticketVazioNaoDividePorZeroEAnteriorZeradoViraNovo() {
		Restaurante casa = restaurante(1L, "Casa");
		PainelAdminDTO.Response vazio = PainelAdmin.montar(
				List.of(casa), List.of(), List.of(), List.of(), List.of(),
				false, DE, ATE, LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 22));
		assertNull(kpi(vazio, "ticket").valor());
		assertEquals("—", kpi(vazio, "ticket").variacao().texto());
		assertEquals("—", kpi(vazio, "faturamento").variacao().texto());

		Produto prato = produto(1L, "Prato");
		List<Pedido> atuais = List.of(pedido(casa, DE, StatusPedido.PAGO, prato, 1, "20.00", "10.00"));
		PainelAdminDTO.Response novo = PainelAdmin.montar(
				List.of(casa), atuais, List.of(), atuais, List.of(),
				false, DE, ATE, LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 22));
		assertEquals("Novo", kpi(novo, "faturamento").variacao().texto());
		assertTrue(novo.alertas().stream().anyMatch(alerta -> alerta.texto().contains("Poucos pedidos")));
		assertTrue(novo.restaurantes().get(0).amostraPequena());
		assertEquals(0, novo.restaurantesNaConsolidacao());
	}

	@Test
	void variacaoUsaPeriodoDoMesmoTamanhoEQuedaViraAlerta() {
		Restaurante casa = restaurante(1L, "Pizzaria Boa Massa");
		Produto pizza = produto(1L, "Pizza");
		List<Pedido> anteriores = pedidos(casa, pizza, LocalDate.of(2026, 9, 23), 10, "10.00", "0.00");
		List<Pedido> atuais = pedidos(casa, pizza, DE, 10, "6.00", "0.00");

		PainelAdminDTO.Response painel = PainelAdmin.montar(
				List.of(casa), atuais, anteriores, atuais, List.of(new PainelAdmin.Cardapio(1L, 4, 0)),
				false, DE, ATE, LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 22));

		assertEquals(new BigDecimal("-40.00"), kpi(painel, "lucro").variacao().percentual());
		assertEquals("DESCE", kpi(painel, "lucro").variacao().sentido());
		assertTrue(painel.alertas().stream().anyMatch(alerta ->
				"CRITICO".equals(alerta.severidade()) && alerta.texto().contains("perdeu 40% de lucro")));
		assertTrue(painel.alertas().stream().noneMatch(alerta -> alerta.texto().contains("Poucos pedidos")));
	}

	@Test
	void dependenciaECustoIncompletoGeramAlertas() {
		Restaurante forte = restaurante(1L, "Itadakimasu Sushi");
		Restaurante fraco = restaurante(2L, "Casa");
		Produto sushi = produto(1L, "Sushi");
		Produto suco = produto(2L, "Suco");
		List<Pedido> atuais = new ArrayList<>();
		atuais.addAll(pedidos(forte, sushi, DE, 5, "16.00", "0.00"));
		atuais.addAll(pedidos(fraco, suco, DE, 5, "4.00", null));

		PainelAdminDTO.Response painel = PainelAdmin.montar(
				List.of(forte, fraco), atuais, List.of(), atuais,
				List.of(new PainelAdmin.Cardapio(2L, 2, 2)),
				true, DE, ATE, null, null);

		assertTrue(painel.alertas().get(0).texto().contains("concentra"));
		assertEquals("CRITICO", painel.alertas().get(0).severidade());
		assertTrue(painel.alertas().stream().anyMatch(alerta ->
				alerta.texto().contains("Casa tem 100% dos produtos vendidos sem custo")));
		assertTrue(kpi(painel, "restaurantes").detalhes().isEmpty());
	}

	@Test
	void restauranteAtivoSemVendaRecenteEntraNumAlertaSo() {
		Restaurante alfa = restaurante(1L, "Alfa");
		Restaurante beta = restaurante(2L, "Beta");
		PainelAdminDTO.Response painel = PainelAdmin.montar(
				List.of(alfa, beta), List.of(), List.of(), List.of(), List.of(),
				true, DE, ATE, null, null);

		List<PainelAdminDTO.Alerta> parados = painel.alertas().stream()
				.filter(alerta -> alerta.texto().contains("últimos 7 dias"))
				.toList();
		assertEquals(1, parados.size());
		assertTrue(parados.get(0).texto().contains("Alfa e Beta"));
		assertEquals(List.of("Alfa", "Beta"), kpi(painel, "restaurantes").detalhes());
		assertTrue(kpi(painel, "restaurantes").atencao());
	}

	private static PainelAdminDTO.Kpi kpi(PainelAdminDTO.Response painel, String codigo) {
		return painel.kpis().stream().filter(item -> item.codigo().equals(codigo)).findFirst().orElseThrow();
	}

	private static List<Pedido> pedidos(
			Restaurante restaurante,
			Produto produto,
			LocalDate inicio,
			int quantidade,
			String preco,
			String custo) {
		List<Pedido> lista = new ArrayList<>();
		for (int i = 0; i < quantidade; i++) {
			lista.add(pedido(restaurante, inicio.plusDays(i), StatusPedido.PAGO, produto, 1, preco, custo));
		}
		return lista;
	}

	private static Restaurante restaurante(Long id, String nome) {
		Restaurante restaurante = new Restaurante();
		restaurante.setId(id);
		restaurante.setNome(nome);
		return restaurante;
	}

	private static Produto produto(Long id, String nome) {
		Categoria categoria = new Categoria();
		categoria.setNome("Cardápio");
		Produto produto = new Produto();
		produto.setId(id);
		produto.setNome(nome);
		produto.setCategoria(categoria);
		return produto;
	}

	private static Pedido pedido(
			Restaurante restaurante,
			LocalDate dia,
			StatusPedido status,
			Produto produto,
			int quantidade,
			String preco,
			String custo) {
		Pedido pedido = new Pedido();
		pedido.setRestaurante(restaurante);
		pedido.setStatus(status);
		pedido.setDataHora(ZonedDateTime.of(dia.getYear(), dia.getMonthValue(), dia.getDayOfMonth(), 12, 0, 0, 0, FUSO)
				.toInstant());
		PedidoItem item = new PedidoItem();
		item.setPedido(pedido);
		item.setProduto(produto);
		item.setQuantidade(quantidade);
		item.setPrecoUnitario(new BigDecimal(preco));
		item.setCustoUnitario(custo == null ? null : new BigDecimal(custo));
		pedido.getItens().add(item);
		return pedido;
	}
}
