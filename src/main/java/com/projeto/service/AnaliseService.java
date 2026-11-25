package com.projeto.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.analise.ClassificadorCardapio;
import com.projeto.analise.LeituraVendas;
import com.projeto.analise.PeriodoConsulta;
import com.projeto.dto.AnaliseDTO;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.Pedido;
import com.projeto.entity.Restaurante;
import com.projeto.repository.PedidoRepository;
import com.projeto.seguranca.EscopoOperacao;

@Service
@Transactional(readOnly = true)
public class AnaliseService {

	private final PedidoRepository pedidoRepository;
	private final EscopoOperacao escopo;
	private final Autorizacao autorizacao;

	public AnaliseService(PedidoRepository pedidoRepository, EscopoOperacao escopo, Autorizacao autorizacao) {
		this.pedidoRepository = pedidoRepository;
		this.escopo = escopo;
		this.autorizacao = autorizacao;
	}

	public AnaliseDTO.Response consultar(LocalDate de, LocalDate ate) {
		PeriodoConsulta.Intervalo periodo = PeriodoConsulta.exigir(de, ate);
		Restaurante restaurante = escopo.restauranteDoContexto();

		List<Pedido> pedidos = pedidoRepository.findByRestauranteIdAndDataHoraGreaterThanEqualAndDataHoraLessThan(
				restaurante.getId(), periodo.inicio(), periodo.fim());

		List<ClassificadorCardapio.Linha> linhas = LeituraVendas.linhasAtivas(pedidos);

		long quantidadePedidos = LeituraVendas.contarPedidosAtivos(pedidos);
		
		LeituraVendas.Resumo resumo = LeituraVendas.resumir(linhas);
		java.util.Map<Long, BigDecimal> custos = LeituraVendas.custoPorProduto(linhas);
		BigDecimal receita = resumo.faturamentoTotal();
		
		BigDecimal ticket = quantidadePedidos == 0
				? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
				: receita.divide(BigDecimal.valueOf(quantidadePedidos), 2, RoundingMode.HALF_UP);
		ClassificadorCardapio.Resultado resultado = ClassificadorCardapio.resultado(linhas);
		List<AnaliseDTO.Item> itens = resultado.itens().stream()
				.map(item -> new AnaliseDTO.Item(
						item.produtoId(),
						item.nome(),
						item.categoria(),
						item.quantidade(),
						item.popularidade(),
						item.margem(),
						item.classe(),
						item.recomendacao(),
						item.margemPercentual(),
						custos.get(item.produtoId())))
				.toList();
		AnaliseDTO.Response cheio = new AnaliseDTO.Response(
				quantidadePedidos,
				receita,
				ticket,
				resultado.limitePopularidade(),
				resultado.margemMedia(),
				itens,
				resumo.custoBruto(),
				resumo.coberturaDeCusto());
		return aplicarVisao(cheio);
	}

	public AnaliseDTO.Response exportar(LocalDate de, LocalDate ate) {
		autorizacao.exigir(AuthorityFuncao.ANALISE_VER_FINANCEIRO);
		return consultar(de, ate);
	}

	private AnaliseDTO.Response aplicarVisao(AnaliseDTO.Response cheio) {
		boolean financeiro = autorizacao.tem(AuthorityFuncao.ANALISE_VER_FINANCEIRO);
		boolean operacional = autorizacao.tem(AuthorityFuncao.ANALISE_VER_OPERACIONAL);
		if (!financeiro && !operacional) {
			throw new AccessDeniedException("Acesso negado");
		}
		if (financeiro) {
			return cheio;
		}
		List<AnaliseDTO.Item> itens = cheio.itens().stream()
				.map(item -> new AnaliseDTO.Item(
						item.produtoId(),
						item.nome(),
						item.categoria(),
						item.quantidade(),
						null,
						null,
						item.classe(),
						item.recomendacao(),
						null,
						null))
				.toList();
		return new AnaliseDTO.Response(cheio.quantidadePedidos(), null, null, null, null, itens, null, null);
	}
}
