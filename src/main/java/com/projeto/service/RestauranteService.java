package com.projeto.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.RestauranteDTO;
import com.projeto.entity.Categoria;
import com.projeto.entity.Comanda;
import com.projeto.entity.Pedido;
import com.projeto.entity.Produto;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.Perfil;
import com.projeto.entity.enums.StatusRestaurante;
import com.projeto.repository.CategoriaRepository;
import com.projeto.repository.ComandaRepository;
import com.projeto.repository.PedidoRepository;
import com.projeto.repository.ProdutoRepository;
import com.projeto.repository.RestauranteRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RecursoNaoEncontradoException;
import com.projeto.util.exception.RegraDeNegocioException;

@Service
@Transactional(readOnly = true)
public class RestauranteService {

	private final RestauranteRepository restauranteRepository;
	private final UsuarioRepository usuarioRepository;
	private final PedidoRepository pedidoRepository;
	private final ComandaRepository comandaRepository;
	private final ProdutoRepository produtoRepository;
	private final CategoriaRepository categoriaRepository;
	private final EscopoOperacao escopo;

	public RestauranteService(
			RestauranteRepository restauranteRepository,
			UsuarioRepository usuarioRepository,
			PedidoRepository pedidoRepository,
			ComandaRepository comandaRepository,
			ProdutoRepository produtoRepository,
			CategoriaRepository categoriaRepository,
			EscopoOperacao escopo) {
		this.restauranteRepository = restauranteRepository;
		this.usuarioRepository = usuarioRepository;
		this.pedidoRepository = pedidoRepository;
		this.comandaRepository = comandaRepository;
		this.produtoRepository = produtoRepository;
		this.categoriaRepository = categoriaRepository;
		this.escopo = escopo;
	}

	public List<RestauranteDTO.Response> listar() {
		Usuario admin = exigirAdmin();
		return restauranteRepository.findByAdminIdOrderByNomeAsc(admin.getId()).stream()
				.map(RestauranteDTO.Response::de)
				.toList();
	}

	@Transactional
	public RestauranteDTO.Response criar(RestauranteDTO.Create dto) {
		Usuario admin = exigirAdmin();
		Restaurante restaurante = new Restaurante();
		restaurante.setNome(dto.nome().trim());
		restaurante.setAdmin(admin);
		restaurante.setStatus(StatusRestaurante.ATIVO);
		return RestauranteDTO.Response.de(restauranteRepository.save(restaurante));
	}

	@Transactional
	public RestauranteDTO.Response atualizar(Long id, RestauranteDTO.Update dto) {
		Restaurante restaurante = carregarEditavel(id);
		restaurante.setNome(dto.nome().trim());
		return RestauranteDTO.Response.de(restauranteRepository.save(restaurante));
	}

	@Transactional
	public RestauranteDTO.Response inativar(Long id) {
		Restaurante restaurante = carregarDoAdmin(id);
		if (restaurante.getStatus() == StatusRestaurante.FECHADO) {
			throw new RegraDeNegocioException("Restaurante fechado não pode ser editado");
		}
		if (restaurante.getStatus() == StatusRestaurante.INATIVO) {
			throw new RegraDeNegocioException("Restaurante já está inativo");
		}
		restaurante.setStatus(StatusRestaurante.INATIVO);
		restaurante.setInativadoPeloAdmin(false);
		inativarEquipe(restaurante.getId());
		return RestauranteDTO.Response.de(restauranteRepository.save(restaurante));
	}

	@Transactional
	public RestauranteDTO.Response reativar(Long id) {
		Restaurante restaurante = carregarDoAdmin(id);
		if (restaurante.getStatus() == StatusRestaurante.FECHADO) {
			throw new RegraDeNegocioException("Restaurante fechado não pode ser reativado");
		}
		if (restaurante.getStatus() == StatusRestaurante.ATIVO) {
			throw new RegraDeNegocioException("Restaurante já está ativo");
		}
		restaurante.setStatus(StatusRestaurante.ATIVO);
		restaurante.setInativadoPeloAdmin(false);
		reativarEquipe(restaurante.getId());
		return RestauranteDTO.Response.de(restauranteRepository.save(restaurante));
	}

	@Transactional
	public RestauranteDTO.Response fechar(Long id) {
		Restaurante restaurante = carregarDoAdmin(id);
		if (restaurante.getStatus() == StatusRestaurante.FECHADO) {
			throw new RegraDeNegocioException("Restaurante já está fechado");
		}
		restaurante.setStatus(StatusRestaurante.FECHADO);
		restaurante.setInativadoPeloAdmin(false);
		inativarEquipe(restaurante.getId());
		return RestauranteDTO.Response.de(restauranteRepository.save(restaurante));
	}

	@Transactional
	public void apagar(Long id) {
		Restaurante restaurante = carregarDoAdmin(id);
		apagarDados(restaurante);
	}

	@Transactional
	public void apagarDoAdmin(Usuario admin) {
		List<Restaurante> restaurantes = restauranteRepository.findByAdminIdOrderByNomeAsc(admin.getId());
		for (Restaurante restaurante : restaurantes) {
			apagarDados(restaurante);
		}
	}

	private void apagarDados(Restaurante restaurante) {
		Long id = restaurante.getId();
		List<Pedido> pedidos = pedidoRepository.findByRestauranteId(id);
		pedidoRepository.deleteAll(pedidos);
		pedidoRepository.flush();
		List<Comanda> comandas = comandaRepository.findByRestauranteId(id);
		comandaRepository.deleteAll(comandas);
		comandaRepository.flush();
		List<Produto> produtos = produtoRepository.findByRestauranteId(id);
		produtoRepository.deleteAll(produtos);
		produtoRepository.flush();
		List<Categoria> categorias = categoriaRepository.findByRestauranteId(id);
		categoriaRepository.deleteAll(categorias);
		categoriaRepository.flush();
		List<Usuario> equipe = usuarioRepository.findByRestauranteIdOrderByNomeAsc(id);
		for (Usuario pessoa : equipe) {
			pessoa.setTokenAcesso(null);
		}
		usuarioRepository.deleteAll(equipe);
		usuarioRepository.flush();
		restauranteRepository.delete(restaurante);
		restauranteRepository.flush();
	}

	private void inativarEquipe(Long restauranteId) {
		for (Usuario pessoa : usuarioRepository.findByRestauranteIdOrderByNomeAsc(restauranteId)) {
			if (!pessoa.isAtivo()) {
				continue;
			}
			pessoa.setAtivo(false);
			pessoa.setTokenAcesso(null);
			pessoa.setInativadoPeloRestaurante(true);
			pessoa.setInativadoPeloAdmin(false);
		}
	}

	private void reativarEquipe(Long restauranteId) {
		for (Usuario pessoa : usuarioRepository.findByRestauranteIdOrderByNomeAsc(restauranteId)) {
			if (!pessoa.isInativadoPeloRestaurante()) {
				continue;
			}
			pessoa.setAtivo(true);
			pessoa.setInativadoPeloRestaurante(false);
		}
	}

	private Restaurante carregarEditavel(Long id) {
		Restaurante restaurante = carregarDoAdmin(id);
		if (restaurante.getStatus() == StatusRestaurante.FECHADO) {
			throw new RegraDeNegocioException("Restaurante fechado não pode ser editado");
		}
		return restaurante;
	}

	private Restaurante carregarDoAdmin(Long id) {
		Usuario admin = exigirAdmin();
		return restauranteRepository.findById(id)
				.filter(restaurante -> restaurante.getAdmin().getId().equals(admin.getId()))
				.orElseThrow(() -> new RecursoNaoEncontradoException("Restaurante não encontrado"));
	}

	private Usuario exigirAdmin() {
		Usuario atual = escopo.usuario();
		// vinculo, nao funcao
		if (atual.getPerfil() != Perfil.ADMINISTRADOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		return atual;
	}
}
