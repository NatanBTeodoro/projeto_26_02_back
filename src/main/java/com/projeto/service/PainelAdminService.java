package com.projeto.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.analise.LimitesPainelAdmin;
import com.projeto.analise.PainelAdmin;
import com.projeto.analise.PeriodoConsulta;
import com.projeto.dto.PainelAdminDTO;
import com.projeto.entity.Produto;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.StatusRestaurante;
import com.projeto.repository.PedidoRepository;
import com.projeto.repository.ProdutoRepository;
import com.projeto.repository.RestauranteRepository;
import com.projeto.seguranca.EscopoOperacao;

@Service
@Transactional(readOnly = true)
public class PainelAdminService {

	private final PedidoRepository pedidoRepository;
	private final RestauranteRepository restauranteRepository;
	private final ProdutoRepository produtoRepository;
	private final EscopoOperacao escopo;

	public PainelAdminService(
			PedidoRepository pedidoRepository,
			RestauranteRepository restauranteRepository,
			ProdutoRepository produtoRepository,
			EscopoOperacao escopo) {
		this.pedidoRepository = pedidoRepository;
		this.restauranteRepository = restauranteRepository;
		this.produtoRepository = produtoRepository;
		this.escopo = escopo;
	}

	public PainelAdminDTO.Response consultar(LocalDate de, LocalDate ate) {
		Usuario admin = EscopoOperacao.administrador(escopo.usuario());
		PeriodoConsulta.Intervalo periodo = PeriodoConsulta.exigir(de, ate);
		PeriodoConsulta.Intervalo anterior = PeriodoConsulta.anterior(de, ate);
		LocalDate hoje = LocalDate.now(PeriodoConsulta.FUSO);
		Instant inicioRecente = PeriodoConsulta.inicio(hoje.minusDays(LimitesPainelAdmin.DIAS_SEM_VENDA - 1L));
		Instant fimRecente = PeriodoConsulta.fimExclusivo(hoje);

		List<Restaurante> ativos = restauranteRepository.findByAdminIdAndStatusOrderByNomeAsc(
				admin.getId(), StatusRestaurante.ATIVO);
		List<Long> ids = ativos.stream().map(Restaurante::getId).toList();
		Instant primeira = ids.isEmpty() ? null : pedidoRepository.primeiraVendaAtiva(ids);
		boolean semPeriodoAnterior = primeira == null || !primeira.isBefore(periodo.inicio());
		List<com.projeto.entity.Pedido> atuais = ids.isEmpty()
				? List.of()
				: pedidoRepository.findByRestauranteIdInAndDataHoraGreaterThanEqualAndDataHoraLessThan(ids, periodo.inicio(), periodo.fim());
		List<com.projeto.entity.Pedido> anteriores = ids.isEmpty() || semPeriodoAnterior
				? List.of()
				: pedidoRepository.findByRestauranteIdInAndDataHoraGreaterThanEqualAndDataHoraLessThan(
						ids, anterior.inicio(), anterior.fim());
		List<com.projeto.entity.Pedido> recentes = ids.isEmpty()
				? List.of()
				: pedidoRepository.findByRestauranteIdInAndDataHoraGreaterThanEqualAndDataHoraLessThan(
						ids, inicioRecente, fimRecente);
		List<PainelAdmin.Cardapio> cardapios = cardapios(ids);
		return PainelAdmin.montar(
				ativos,
				atuais,
				anteriores,
				recentes,
				cardapios,
				semPeriodoAnterior,
				de,
				ate,
				anterior.de(),
				anterior.ate());
	}

	private List<PainelAdmin.Cardapio> cardapios(List<Long> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		java.util.Map<Long, int[]> contagem = new java.util.HashMap<>();
		for (Long id : ids) {
			contagem.put(id, new int[2]);
		}
		for (Produto produto : produtoRepository.findByRestauranteIdIn(ids)) {
			if (!produto.isAtivo() || produto.getRestaurante() == null) {
				continue;
			}
			int[] valores = contagem.get(produto.getRestaurante().getId());
			if (valores == null) {
				continue;
			}
			valores[0]++;
			if (produto.getCustoAtual() == null) {
				valores[1]++;
			}
		}
		return contagem.entrySet().stream()
				.map(entrada -> new PainelAdmin.Cardapio(entrada.getKey(), entrada.getValue()[0], entrada.getValue()[1]))
				.toList();
	}
}
