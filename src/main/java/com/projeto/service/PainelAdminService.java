package com.projeto.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.PainelAdminDTO;
import com.projeto.entity.Produto;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.Perfil;
import com.projeto.entity.enums.StatusRestaurante;
import com.projeto.repository.PedidoRepository;
import com.projeto.repository.ProdutoRepository;
import com.projeto.repository.RestauranteRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RegraDeNegocioException;

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
		Usuario admin = exigirAdmin();
		if (de == null || ate == null) {
			throw new RegraDeNegocioException("Informe a data inicial e a data final");
		}
		if (de.isAfter(ate)) {
			throw new RegraDeNegocioException("A data inicial precisa ser anterior ou igual à final");
		}
		long dias = ChronoUnit.DAYS.between(de, ate) + 1;
		LocalDate ateAnterior = de.minusDays(1);
		LocalDate deAnterior = ateAnterior.minusDays(dias - 1);
		Instant inicio = de.atStartOfDay(AnaliseService.FUSO).toInstant();
		Instant fim = ate.plusDays(1).atStartOfDay(AnaliseService.FUSO).toInstant();
		Instant inicioAnterior = deAnterior.atStartOfDay(AnaliseService.FUSO).toInstant();
		Instant fimAnterior = ateAnterior.plusDays(1).atStartOfDay(AnaliseService.FUSO).toInstant();
		LocalDate hoje = LocalDate.now(AnaliseService.FUSO);
		Instant inicioRecente = hoje.minusDays(LimitesPainelAdmin.DIAS_SEM_VENDA - 1L)
				.atStartOfDay(AnaliseService.FUSO)
				.toInstant();
		Instant fimRecente = hoje.plusDays(1).atStartOfDay(AnaliseService.FUSO).toInstant();

		List<Restaurante> ativos = restauranteRepository.findByAdminIdAndStatusOrderByNomeAsc(
				admin.getId(), StatusRestaurante.ATIVO);
		List<Long> ids = ativos.stream().map(Restaurante::getId).toList();
		Instant primeira = ids.isEmpty() ? null : pedidoRepository.primeiraVendaAtiva(ids);
		boolean semPeriodoAnterior = primeira == null || !primeira.isBefore(inicio);
		List<com.projeto.entity.Pedido> atuais = ids.isEmpty()
				? List.of()
				: pedidoRepository.findByRestauranteIdInAndDataHoraGreaterThanEqualAndDataHoraLessThan(ids, inicio, fim);
		List<com.projeto.entity.Pedido> anteriores = ids.isEmpty() || semPeriodoAnterior
				? List.of()
				: pedidoRepository.findByRestauranteIdInAndDataHoraGreaterThanEqualAndDataHoraLessThan(
						ids, inicioAnterior, fimAnterior);
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
				deAnterior,
				ateAnterior);
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

	private Usuario exigirAdmin() {
		Usuario atual = escopo.usuario();
		if (atual.getPerfil() != Perfil.ADMINISTRADOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		return atual;
	}
}
