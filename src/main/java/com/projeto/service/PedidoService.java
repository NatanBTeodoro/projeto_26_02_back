package com.projeto.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.PedidoDTO;
import com.projeto.entity.Comanda;
import com.projeto.entity.Pedido;
import com.projeto.entity.PedidoAlteracao;
import com.projeto.entity.PedidoItem;
import com.projeto.entity.Produto;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.Perfil;
import com.projeto.entity.enums.StatusPedido;
import com.projeto.repository.ComandaRepository;
import com.projeto.repository.PedidoRepository;
import com.projeto.repository.ProdutoRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RecursoNaoEncontradoException;
import com.projeto.util.exception.RegraDeNegocioException;

@Service
@Transactional(readOnly = true)
public class PedidoService {

	private final PedidoRepository pedidoRepository;
	private final ProdutoRepository produtoRepository;
	private final ComandaRepository comandaRepository;
	private final ComandaService comandaService;
	private final UsuarioRepository usuarioRepository;
	private final EscopoOperacao escopo;

	public PedidoService(
			PedidoRepository pedidoRepository,
			ProdutoRepository produtoRepository,
			ComandaRepository comandaRepository,
			ComandaService comandaService,
			UsuarioRepository usuarioRepository,
			EscopoOperacao escopo) {
		this.pedidoRepository = pedidoRepository;
		this.produtoRepository = produtoRepository;
		this.comandaRepository = comandaRepository;
		this.comandaService = comandaService;
		this.usuarioRepository = usuarioRepository;
		this.escopo = escopo;
	}

	public List<PedidoDTO.Response> listar() {
		Usuario atual = escopo.usuario();
		if (atual.getPerfil() == Perfil.USUARIO) {
			return pedidoRepository.findByUsuarioIdOrderByDataHoraDesc(atual.getId()).stream()
					.map(PedidoDTO.Response::de)
					.toList();
		}
		if (atual.getPerfil() == Perfil.GESTOR || atual.getPerfil() == Perfil.ADMINISTRADOR) {
			Restaurante restaurante = escopo.restauranteDoContexto();
			return pedidoRepository.findByRestauranteIdOrderByDataHoraDesc(restaurante.getId()).stream()
					.map(PedidoDTO.Response::de)
					.toList();
		}
		throw new AccessDeniedException("Acesso negado");
	}

	@Transactional
	public PedidoDTO.Response criar(PedidoDTO.Create dto) {
		Instant agora = Instant.now();
		RegrasPedido.validarVenda(dto.dataHora(), dto.mesa(), dto.nomeCliente(), dto.itens(), agora);
		Usuario atual = escopo.usuario();
		Restaurante restaurante = escopo.exigirOperavel();
		Usuario autorPedido = autorDoPedido(atual, restaurante, dto.usuarioId());
		Comanda comanda = resolverComanda(atual, restaurante, autorPedido, dto.comandaId());
		Pedido pedido = new Pedido();
		pedido.setDataHora(dto.dataHora() == null ? agora : dto.dataHora());
		pedido.setStatus(StatusPedido.ATIVO);
		pedido.setRestaurante(restaurante);
		pedido.setUsuario(autorPedido);
		pedido.setUsuarioNome(autorPedido.getNome());
		pedido.setLancadoPor(RotuloAutor.noPedido(atual.getPerfil(), atual.getNome(), atual.getEmail()));
		pedido.setComanda(comanda);
		pedido.setMesa(dto.mesa());
		pedido.setNomeCliente(dto.nomeCliente().trim());
		comandaService.lembrarCliente(comanda, pedido.getMesa(), pedido.getNomeCliente());
		preencherItens(pedido, agrupar(dto.itens()), Map.of(), restaurante);
		return PedidoDTO.Response.de(pedidoRepository.save(pedido));
	}

	@Transactional
	public PedidoDTO.Response estornar(Long id, PedidoDTO.Estorno dto) {
		Pedido pedido = carregarMutavel(id, true);
		pedido.setStatus(StatusPedido.ESTORNADO);
		pedido.setMotivoEstorno(dto.motivo().trim());
		pedido.setDataHoraEstorno(Instant.now());
		pedido.setAutorEstorno(escopo.autor());
		return PedidoDTO.Response.de(pedidoRepository.save(pedido));
	}

	@Transactional
	public PedidoDTO.Response cancelar(Long id, PedidoDTO.Cancelamento dto) {
		Pedido pedido = carregarMutavel(id, false);
		pedido.setStatus(StatusPedido.CANCELADO);
		pedido.setMotivoCancelamento(dto.motivo().trim());
		pedido.setDataHoraCancelamento(Instant.now());
		pedido.setAutorCancelamento(escopo.autor());
		return PedidoDTO.Response.de(pedidoRepository.save(pedido));
	}

	@Transactional
	public PedidoDTO.Response editar(Long id, PedidoDTO.Edicao dto) {
		Pedido pedido = carregarMutavel(id, false);
		Instant agora = Instant.now();
		RegrasPedido.validarVenda(null, dto.mesa(), dto.nomeCliente(), dto.itens(), agora);
		Map<Long, Integer> quantidades = agrupar(dto.itens());
		String descricao = descrever(pedido, dto.mesa(), dto.nomeCliente().trim(), quantidades);
		Map<Long, PrecoGravado> anteriores = precos(pedido);
		pedido.getItens().clear();
		preencherItens(pedido, quantidades, anteriores, pedido.getRestaurante());
		pedido.setMesa(dto.mesa());
		pedido.setNomeCliente(dto.nomeCliente().trim());
		if (pedido.getComanda() != null) {
			comandaService.lembrarCliente(pedido.getComanda(), pedido.getMesa(), pedido.getNomeCliente());
		}
		PedidoAlteracao alteracao = new PedidoAlteracao();
		alteracao.setPedido(pedido);
		alteracao.setDataHora(agora);
		alteracao.setDescricao(limitar(descricao, 1000));
		alteracao.setMotivo(dto.motivo().trim());
		alteracao.setAutor(escopo.autor());
		pedido.getAlteracoes().add(alteracao);
		return PedidoDTO.Response.de(pedidoRepository.save(pedido));
	}

	static Map<Long, Integer> agrupar(List<PedidoDTO.ItemCreate> itens) {
		Map<Long, Integer> quantidades = new LinkedHashMap<>();
		for (PedidoDTO.ItemCreate item : itens) {
			quantidades.merge(item.produtoId(), item.quantidade(), Math::addExact);
		}
		return quantidades;
	}

	private void preencherItens(
			Pedido pedido,
			Map<Long, Integer> quantidades,
			Map<Long, PrecoGravado> anteriores,
			Restaurante restaurante) {
		for (Map.Entry<Long, Integer> entrada : quantidades.entrySet()) {
			Produto produto = produtoRepository.findById(entrada.getKey())
					.orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));
			if (produto.getRestaurante() == null || !produto.getRestaurante().getId().equals(restaurante.getId())) {
				throw new RecursoNaoEncontradoException("Produto não encontrado");
			}
			PrecoGravado gravado = anteriores.get(produto.getId());
			if (gravado == null && !produto.isAtivo()) {
				throw new RegraDeNegocioException("Produto inativo não entra em pedido novo");
			}
			PedidoItem linha = new PedidoItem();
			linha.setPedido(pedido);
			linha.setProduto(produto);
			linha.setQuantidade(entrada.getValue());
			linha.setPrecoUnitario(gravado == null ? produto.getPrecoAtual() : gravado.preco());
			linha.setCustoUnitario(gravado == null ? produto.getCustoAtual() : gravado.custo());
			pedido.getItens().add(linha);
		}
	}

	private Usuario autorDoPedido(Usuario atual, Restaurante restaurante, Long usuarioId) {
		if (atual.getPerfil() == Perfil.USUARIO) {
			return atual;
		}
		if (atual.getPerfil() != Perfil.GESTOR && atual.getPerfil() != Perfil.ADMINISTRADOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		if (usuarioId == null) {
			throw new RegraDeNegocioException("Informe o usuário");
		}
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
		if (usuario.getPerfil() != Perfil.USUARIO
				|| !usuario.isAtivo()
				|| usuario.getRestaurante() == null
				|| !usuario.getRestaurante().getId().equals(restaurante.getId())) {
			throw new RegraDeNegocioException("Usuário inválido para o pedido");
		}
		return usuario;
	}

	private Comanda resolverComanda(Usuario atual, Restaurante restaurante, Usuario autorPedido, Long comandaId) {
		if (atual.getPerfil() == Perfil.USUARIO && comandaId == null) {
			throw new RegraDeNegocioException("Abra uma comanda antes de lançar o pedido");
		}
		if (comandaId == null) {
			return comandaService.nova(restaurante, autorPedido);
		}
		Comanda comanda = comandaRepository.findById(comandaId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Comanda não encontrada"));
		if (!comanda.getRestaurante().getId().equals(restaurante.getId())) {
			throw new AccessDeniedException("Acesso negado");
		}
		if (comanda.getUsuario() == null || !comanda.getUsuario().getId().equals(autorPedido.getId())) {
			throw new RegraDeNegocioException("A comanda não pertence a este usuário");
		}
		if (!comanda.isAberta()) {
			throw new RegraDeNegocioException("Comanda fechada");
		}
		return comanda;
	}

	private Pedido carregarMutavel(Long id, boolean aceitaPago) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
		boolean aberto = pedido.getStatus() == StatusPedido.ATIVO;
		boolean pago = aceitaPago && pedido.getStatus() == StatusPedido.PAGO;
		if (!aberto && !pago) {
			throw new RegraDeNegocioException("Pedido já encerrado");
		}
		Usuario atual = escopo.usuario();
		if (atual.getPerfil() != Perfil.GESTOR && atual.getPerfil() != Perfil.ADMINISTRADOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		Restaurante contexto = escopo.exigirOperavel();
		if (pedido.getRestaurante() == null || !pedido.getRestaurante().getId().equals(contexto.getId())) {
			throw new AccessDeniedException("Acesso negado");
		}
		return pedido;
	}

	private String descrever(Pedido pedido, Integer mesa, String cliente, Map<Long, Integer> quantidades) {
		List<String> partes = new ArrayList<>();
		if (!Objects.equals(pedido.getMesa(), mesa)) {
			partes.add("Mesa: " + pedido.getMesa() + " → " + mesa);
		}
		if (!Objects.equals(pedido.getNomeCliente(), cliente)) {
			partes.add("Cliente: " + pedido.getNomeCliente() + " → " + cliente);
		}
		String antes = textoItens(pedido);
		String depois = textoNovos(quantidades);
		if (!antes.equals(depois)) {
			partes.add("Itens: " + antes + " → " + depois);
		}
		if (partes.isEmpty()) {
			throw new RegraDeNegocioException("Nenhuma alteração no pedido");
		}
		return String.join("; ", partes);
	}

	private String textoItens(Pedido pedido) {
		List<String> partes = new ArrayList<>();
		for (PedidoItem item : pedido.getItens()) {
			partes.add(item.getQuantidade() + " × " + item.getProduto().getNome());
		}
		return String.join(", ", partes);
	}

	private String textoNovos(Map<Long, Integer> quantidades) {
		List<String> partes = new ArrayList<>();
		for (Map.Entry<Long, Integer> entrada : quantidades.entrySet()) {
			Produto produto = produtoRepository.findById(entrada.getKey())
					.orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));
			partes.add(entrada.getValue() + " × " + produto.getNome());
		}
		return String.join(", ", partes);
	}

	private Map<Long, PrecoGravado> precos(Pedido pedido) {
		Map<Long, PrecoGravado> gravados = new LinkedHashMap<>();
		for (PedidoItem item : pedido.getItens()) {
			gravados.put(item.getProduto().getId(), new PrecoGravado(item.getPrecoUnitario(), item.getCustoUnitario()));
		}
		return gravados;
	}

	private String limitar(String texto, int maximo) {
		if (texto.length() <= maximo) {
			return texto;
		}
		return texto.substring(0, maximo);
	}

	private record PrecoGravado(BigDecimal preco, BigDecimal custo) {
	}
}
