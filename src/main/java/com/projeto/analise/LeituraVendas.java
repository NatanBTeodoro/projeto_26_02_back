package com.projeto.analise;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.projeto.entity.Pedido;
import com.projeto.entity.PedidoItem;
import com.projeto.entity.enums.StatusPedido;

public final class LeituraVendas {

	private LeituraVendas() {
	}

	public static List<ClassificadorCardapio.Linha> linhasAtivas(List<Pedido> pedidos) {
		List<ClassificadorCardapio.Linha> linhas = new ArrayList<>();
		for (Pedido pedido : pedidos) {
			if (pedido.getStatus() != StatusPedido.PAGO) {
				continue;
			}
			for (PedidoItem item : pedido.getItens()) {
				String categoria = item.getProduto().getCategoria() == null
						? ""
						: item.getProduto().getCategoria().getNome();
				linhas.add(new ClassificadorCardapio.Linha(
						item.getProduto().getId(),
						item.getProduto().getNome(),
						item.getQuantidade(),
						item.getPrecoUnitario(),
						item.getCustoUnitario(),
						categoria));
			}
		}
		return linhas;
	}

	public static long contarPedidosAtivos(List<Pedido> pedidos) {
		return pedidos.stream().filter(pedido -> pedido.getStatus() == StatusPedido.PAGO).count();
	}

	public record Resumo(
			BigDecimal faturamentoTotal,
			BigDecimal faturamentoComCusto,
			BigDecimal custoBruto,
			BigDecimal lucroBruto,
			BigDecimal coberturaDeCusto) {
	}

	public static Resumo resumir(List<ClassificadorCardapio.Linha> linhas) {
		BigDecimal faturamento = BigDecimal.ZERO;
		BigDecimal comCusto = BigDecimal.ZERO;
		BigDecimal custo = BigDecimal.ZERO;
		for (ClassificadorCardapio.Linha linha : linhas) {
			if (linha.preco() == null) {
				continue;
			}
			BigDecimal venda = linha.preco().multiply(BigDecimal.valueOf(linha.quantidade()));
			faturamento = faturamento.add(venda);
			if (linha.custo() == null) {
				continue;
			}
			comCusto = comCusto.add(venda);
			custo = custo.add(linha.custo().multiply(BigDecimal.valueOf(linha.quantidade())));
		}
		faturamento = faturamento.setScale(2, RoundingMode.HALF_UP);
		comCusto = comCusto.setScale(2, RoundingMode.HALF_UP);
		custo = custo.setScale(2, RoundingMode.HALF_UP);
		BigDecimal lucro = comCusto.subtract(custo).setScale(2, RoundingMode.HALF_UP);
		BigDecimal cobertura = faturamento.signum() == 0
				? null
				: comCusto.multiply(new BigDecimal("100")).divide(faturamento, 2, RoundingMode.HALF_UP);
		return new Resumo(faturamento, comCusto, custo, lucro, cobertura);
	}

	public static Map<Long, BigDecimal> custoPorProduto(List<ClassificadorCardapio.Linha> linhas) {
		Map<Long, BigDecimal> custos = new LinkedHashMap<>();
		for (ClassificadorCardapio.Linha linha : linhas) {
			if (linha.custo() == null) {
				continue;
			}
			BigDecimal parcial = linha.custo().multiply(BigDecimal.valueOf(linha.quantidade()));
			custos.merge(linha.produtoId(), parcial, BigDecimal::add);
		}
		custos.replaceAll((id, valor) -> valor.setScale(2, RoundingMode.HALF_UP));
		return custos;
	}
}
