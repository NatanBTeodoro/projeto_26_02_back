package com.projeto.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.ComandaDTO;
import com.projeto.regra.EscopoVinculo;
import com.projeto.regra.FechamentoComanda;
import com.projeto.regra.RegrasComanda;
import com.projeto.entity.Comanda;
import com.projeto.entity.Pedido;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.OperacaoAuditoria;
import com.projeto.entity.enums.Perfil;
import com.projeto.entity.enums.StatusPedido;
import com.projeto.entity.enums.StatusRestaurante;
import com.projeto.repository.ComandaRepository;
import com.projeto.repository.PedidoRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RecursoNaoEncontradoException;
import com.projeto.util.exception.RegraDeNegocioException;

@Service
@Transactional(readOnly = true)
public class ComandaService {

	private final ComandaRepository comandaRepository;
	private final PedidoRepository pedidoRepository;
	private final UsuarioRepository usuarioRepository;
	private final EscopoOperacao escopo;
	private final AuditoriaFinanceiraService auditoria;
	private final Autorizacao autorizacao;

	public ComandaService(
			ComandaRepository comandaRepository,
			PedidoRepository pedidoRepository,
			UsuarioRepository usuarioRepository,
			EscopoOperacao escopo,
			AuditoriaFinanceiraService auditoria,
			Autorizacao autorizacao) {
		this.comandaRepository = comandaRepository;
		this.pedidoRepository = pedidoRepository;
		this.usuarioRepository = usuarioRepository;
		this.escopo = escopo;
		this.auditoria = auditoria;
		this.autorizacao = autorizacao;
	}

	public List<ComandaDTO.Response> listar(Long usuarioId) {
		autorizacao.exigir(AuthorityFuncao.COMANDA_LER);
		if (!EscopoVinculo.deUnidade(escopo.usuario().getPerfil())) {
			Usuario atual = escopo.usuario();
			return responder(comandaRepository.findByUsuarioIdOrderByIdDesc(atual.getId()));
		}
		Restaurante restaurante = escopo.restauranteDoContexto();
		if (usuarioId == null) {
			throw new RegraDeNegocioException("Informe o usuário");
		}
		Usuario alvo = usuarioDoRestaurante(restaurante, usuarioId);
		return responder(comandaRepository.findByRestauranteIdAndUsuarioIdOrderByIdDesc(restaurante.getId(), alvo.getId()));
	}

	@Transactional
	public ComandaDTO.Response abrir(ComandaDTO.Create dto) {
		Usuario atual = escopo.usuario();
		Restaurante restaurante = escopo.exigirOperavel();
		autorizacao.exigir(AuthorityFuncao.COMANDA_ABRIR);
		Usuario dono;
		if (EscopoVinculo.deUnidade(atual.getPerfil())) {
			if (dto == null || dto.usuarioId() == null) {
				throw new RegraDeNegocioException("Informe o usuário");
			}
			dono = usuarioDoRestaurante(restaurante, dto.usuarioId());
		} else {
			dono = atual;
		}
		Comanda comanda = nova(restaurante, dono);
		return responder(comanda);
	}

	@Transactional
	public Comanda nova(Restaurante restaurante, Usuario dono) {
		Comanda comanda = new Comanda();
		comanda.setRestaurante(restaurante);
		comanda.setUsuario(dono);
		comanda.setCriadaEm(Instant.now());
		comanda.setAberta(true);
		comanda.setNumero(comandaRepository.maiorNumero(restaurante.getId()) + 1);
		return comandaRepository.save(comanda);
	}

	@Transactional
	public ComandaDTO.Response fechar(Long id) {
		Comanda comanda = comandaRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Comanda não encontrada"));
		Restaurante restaurante = escopo.restauranteDoContexto();
		if (comanda.getRestaurante() == null || !comanda.getRestaurante().getId().equals(restaurante.getId())) {
			throw new RecursoNaoEncontradoException("Comanda não encontrada");
		}
		if (restaurante.getStatus() != StatusRestaurante.ATIVO) {
			throw new RegraDeNegocioException("Restaurante fechado ou inativo não pode ser alterado");
		}
		if (!comanda.isAberta()) {
			throw new RegraDeNegocioException("Comanda já fechada");
		}
		autorizacao.exigir(AuthorityFuncao.COMANDA_FECHAR);
		Usuario atual = escopo.usuario();
		List<Pedido> pedidos = pedidoRepository.findByComandaId(comanda.getId());
		List<Map<String, Object>> reconhecidos = new ArrayList<>();
		BigDecimal total = BigDecimal.ZERO;
		for (Pedido pedido : pedidos) {
			if (pedido.getStatus() != StatusPedido.ATIVO) {
				continue;
			}
			BigDecimal valor = AuditoriaFinanceiraService.valor(pedido);
			total = total.add(valor);
			reconhecidos.add(Map.of("pedidoId", pedido.getId(), "valor", valor));
		}
		FechamentoComanda.aplicar(comanda, pedidos);
		pedidoRepository.saveAll(pedidos);
		Comanda salva = comandaRepository.save(comanda);
		auditoria.registrar(
				restaurante.getId(),
				atual,
				OperacaoAuditoria.FECHAMENTO_COMANDA,
				salva.getId(),
				auditoria.json(Map.of("aberta", true, "pedidos", reconhecidos)),
				auditoria.json(Map.of("aberta", false, "valorReconhecido", total, "pedidos", reconhecidos)),
				"pedidosPagos=" + reconhecidos.size());
		return responder(salva);
	}

	@Transactional
	public void lembrarCliente(Comanda comanda, Integer mesa, String nomeCliente) {
		RegrasComanda.exigirMesmaMesa(comanda.getMesa(), mesa);
		String nome = nomeCliente == null ? null : nomeCliente.trim();
		String anterior = comanda.getNomeCliente();
		boolean trocouNome = anterior != null && !anterior.isBlank() && nome != null && !anterior.equals(nome);
		comanda.setMesa(mesa);
		comanda.setNomeCliente(nome);
		comandaRepository.save(comanda);
		if (!trocouNome) {
			return;
		}
		for (Pedido pedido : pedidoRepository.findByComandaId(comanda.getId())) {
			pedido.setNomeCliente(nome);
			pedido.setMesa(mesa);
		}
	}

	public Set<String> nomesEmMaisDeUmaMesa(Long restauranteId) {
		if (restauranteId == null) {
			return Set.of();
		}
		return RegrasComanda.nomesEmMaisDeUmaMesa(comandaRepository.findByRestauranteIdAndAbertaTrue(restauranteId).stream()
				.map(comanda -> new RegrasComanda.Ocupacao(comanda.getNomeCliente(), comanda.getMesa()))
				.toList());
	}

	public String rotulo(String nome, Integer mesa, Set<String> nomesEmMaisDeUmaMesa) {
		return RegrasComanda.rotulo(nome, mesa, RegrasComanda.colide(nome, nomesEmMaisDeUmaMesa));
	}

	private List<ComandaDTO.Response> responder(List<Comanda> comandas) {
		if (comandas.isEmpty()) {
			return List.of();
		}
		Set<String> colisao = nomesEmMaisDeUmaMesa(comandas.get(0).getRestaurante().getId());
		return comandas.stream()
				.map(comanda -> ComandaDTO.Response.de(comanda, rotulo(comanda.getNomeCliente(), comanda.getMesa(), colisao)))
				.toList();
	}

	private ComandaDTO.Response responder(Comanda comanda) {
		Set<String> colisao = nomesEmMaisDeUmaMesa(comanda.getRestaurante().getId());
		return ComandaDTO.Response.de(comanda, rotulo(comanda.getNomeCliente(), comanda.getMesa(), colisao));
	}

	private Usuario usuarioDoRestaurante(Restaurante restaurante, Long usuarioId) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
		if (usuario.getPerfil() != Perfil.USUARIO
				|| usuario.getRestaurante() == null
				|| !usuario.getRestaurante().getId().equals(restaurante.getId())) {
			throw new AccessDeniedException("Acesso negado");
		}
		return usuario;
	}
}
