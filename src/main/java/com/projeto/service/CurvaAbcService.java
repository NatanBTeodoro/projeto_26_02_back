package com.projeto.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.CurvaAbcDTO;
import com.projeto.entity.Pedido;
import com.projeto.entity.PedidoItem;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.Perfil;
import com.projeto.entity.enums.StatusPedido;
import com.projeto.entity.enums.StatusRestaurante;
import com.projeto.repository.PedidoRepository;
import com.projeto.repository.RestauranteRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RegraDeNegocioException;

@Service
@Transactional(readOnly = true)
public class CurvaAbcService {

	private final PedidoRepository pedidoRepository;
	private final RestauranteRepository restauranteRepository;
	private final EscopoOperacao escopo;
	private final ModelosAnaliticos modelosAnaliticos;

	public CurvaAbcService(
			PedidoRepository pedidoRepository,
			RestauranteRepository restauranteRepository,
			EscopoOperacao escopo,
			ModelosAnaliticos modelosAnaliticos) {
		this.pedidoRepository = pedidoRepository;
		this.restauranteRepository = restauranteRepository;
		this.escopo = escopo;
		this.modelosAnaliticos = modelosAnaliticos;
	}

	public List<CurvaAbcDTO.Modelo> modelos() {
		exigirAdmin();
		return modelosAnaliticos.listar().stream()
				.map(modelo -> new CurvaAbcDTO.Modelo(modelo.codigo(), modelo.nome()))
				.toList();
	}

	public CurvaAbcDTO.Response porRestaurante(LocalDate de, LocalDate ate) {
		Usuario admin = exigirAdmin();
		Periodo periodo = periodo(de, ate);
		List<Restaurante> ativos = restauranteRepository.findByAdminIdAndStatusOrderByNomeAsc(
				admin.getId(), StatusRestaurante.ATIVO);
		List<Long> ids = ativos.stream().map(Restaurante::getId).toList();
		List<Pedido> pedidos = ids.isEmpty()
				? List.of()
				: pedidoRepository.findByRestauranteIdInAndDataHoraGreaterThanEqualAndDataHoraLessThan(
						ids, periodo.inicio(), periodo.fim());
		Map<Long, Acumulo> porRestaurante = new LinkedHashMap<>();
		for (Restaurante restaurante : ativos) {
			porRestaurante.put(restaurante.getId(), new Acumulo(String.valueOf(restaurante.getId()), restaurante.getNome()));
		}
		long linhasSemCusto = acumular(pedidos, porRestaurante, item -> item.getPedido().getRestaurante().getId());
		return montar(porRestaurante, linhasSemCusto);
	}

	public CurvaAbcDTO.Response porProduto(LocalDate de, LocalDate ate, Long restauranteId) {
		Usuario admin = exigirAdmin();
		if (restauranteId == null) {
			throw new RegraDeNegocioException("Escolha o restaurante");
		}
		Restaurante restaurante = escopo.carregarDoAdmin(admin, restauranteId);
		Periodo periodo = periodo(de, ate);
		List<Pedido> pedidos = pedidoRepository.findByRestauranteIdAndDataHoraGreaterThanEqualAndDataHoraLessThan(
				restaurante.getId(), periodo.inicio(), periodo.fim());
		Map<Long, Acumulo> porProduto = new LinkedHashMap<>();
		long linhasSemCusto = acumular(pedidos, porProduto, item -> item.getProduto().getId(), true);
		return montar(porProduto, linhasSemCusto);
	}

	private CurvaAbcDTO.Response montar(Map<Long, Acumulo> grupos, long linhasSemCusto) {
		List<CurvaAbc.Entrada> entradas = new ArrayList<>();
		List<String> fora = new ArrayList<>();
		for (Acumulo acumulo : grupos.values()) {
			if (!acumulo.teveCusto) {
				if (acumulo.teveSemCusto) {
					fora.add(acumulo.nome);
					continue;
				}
				entradas.add(new CurvaAbc.Entrada(
						acumulo.id,
						acumulo.nome,
						BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
						false));
				continue;
			}
			entradas.add(new CurvaAbc.Entrada(
					acumulo.id,
					acumulo.nome,
					acumulo.lucro.setScale(2, RoundingMode.HALF_UP),
					acumulo.teveSemCusto));
		}
		fora.sort(String.CASE_INSENSITIVE_ORDER);
		CurvaAbc.Resultado resultado = CurvaAbc.calcular(entradas);
		List<CurvaAbcDTO.Item> itens = resultado.itens().stream()
				.map(item -> new CurvaAbcDTO.Item(
						item.id(),
						item.nome(),
						item.lucro(),
						item.participacao(),
						item.acumulado(),
						item.faixa(),
						item.dadoIncompleto()))
				.toList();
		return new CurvaAbcDTO.Response(
				CurvaAbc.CODIGO,
				resultado.lucroBruto(),
				linhasSemCusto,
				fora,
				itens);
	}

	private long acumular(List<Pedido> pedidos, Map<Long, Acumulo> grupos, java.util.function.Function<PedidoItem, Long> chave) {
		return acumular(pedidos, grupos, chave, false);
	}

	private long acumular(
			List<Pedido> pedidos,
			Map<Long, Acumulo> grupos,
			java.util.function.Function<PedidoItem, Long> chave,
			boolean criarProduto) {
		long semCusto = 0;
		for (Pedido pedido : pedidos) {
			if (pedido.getStatus() != StatusPedido.PAGO) {
				continue;
			}
			for (PedidoItem item : pedido.getItens()) {
				Long id = chave.apply(item);
				Acumulo acumulo = grupos.get(id);
				if (acumulo == null && criarProduto) {
					acumulo = new Acumulo(String.valueOf(id), item.getProduto().getNome());
					grupos.put(id, acumulo);
				}
				if (acumulo == null) {
					continue;
				}
				if (item.getCustoUnitario() == null) {
					acumulo.teveSemCusto = true;
					semCusto++;
					continue;
				}
				BigDecimal lucro = item.getPrecoUnitario()
						.subtract(item.getCustoUnitario())
						.multiply(BigDecimal.valueOf(item.getQuantidade()));
				acumulo.lucro = acumulo.lucro.add(lucro);
				acumulo.teveCusto = true;
			}
		}
		return semCusto;
	}

	private Usuario exigirAdmin() {
		Usuario atual = escopo.usuario();
		if (atual.getPerfil() != Perfil.ADMINISTRADOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		return atual;
	}

	private Periodo periodo(LocalDate de, LocalDate ate) {
		if (de == null || ate == null) {
			throw new RegraDeNegocioException("Informe a data inicial e a data final");
		}
		if (de.isAfter(ate)) {
			throw new RegraDeNegocioException("A data inicial precisa ser anterior ou igual à final");
		}
		Instant inicio = de.atStartOfDay(AnaliseService.FUSO).toInstant();
		Instant fim = ate.plusDays(1).atStartOfDay(AnaliseService.FUSO).toInstant();
		return new Periodo(inicio, fim);
	}

	private record Periodo(Instant inicio, Instant fim) {
	}

	private static final class Acumulo {
		private final String id;
		private final String nome;
		private BigDecimal lucro = BigDecimal.ZERO;
		private boolean teveCusto;
		private boolean teveSemCusto;

		private Acumulo(String id, String nome) {
			this.id = id;
			this.nome = nome;
		}
	}
}
