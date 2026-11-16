package com.projeto.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.ProdutoDTO;
import com.projeto.entity.Produto;
import com.projeto.entity.Restaurante;
import com.projeto.repository.ProdutoRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RecursoNaoEncontradoException;
import com.projeto.util.exception.RegraDeNegocioException;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

	private final ProdutoRepository produtoRepository;
	private final CategoriaService categoriaService;
	private final EscopoOperacao escopo;

	public ProdutoService(
			ProdutoRepository produtoRepository,
			CategoriaService categoriaService,
			EscopoOperacao escopo) {
		this.produtoRepository = produtoRepository;
		this.categoriaService = categoriaService;
		this.escopo = escopo;
	}

	public List<ProdutoDTO.Response> listar() {
		Restaurante restaurante = escopo.restauranteDoContexto();
		return produtoRepository.findByRestauranteIdOrderByNomeAsc(restaurante.getId()).stream()
				.map(ProdutoDTO.Response::de)
				.toList();
	}

	@Transactional
	public ProdutoDTO.Response criar(ProdutoDTO.Create dto) {
		escopo.exigirOperavel();
		Produto produto = new Produto();
		preencher(produto, dto.categoriaId(), dto.nome(), dto.precoAtual(), dto.custoAtual());
		produto.setRestaurante(produto.getCategoria().getRestaurante());
		produto.setAtivo(true);
		return ProdutoDTO.Response.de(produtoRepository.save(produto));
	}

	@Transactional
	public ProdutoDTO.Response atualizar(Long id, ProdutoDTO.Update dto) {
		escopo.exigirOperavel();
		Produto produto = carregar(id);
		preencher(produto, dto.categoriaId(), dto.nome(), dto.precoAtual(), dto.custoAtual());
		produto.setRestaurante(produto.getCategoria().getRestaurante());
		if (dto.ativo() != null) {
			produto.setAtivo(dto.ativo());
		}
		return ProdutoDTO.Response.de(produtoRepository.save(produto));
	}

	private void preencher(
			Produto produto,
			Long categoriaId,
			String nome,
			java.math.BigDecimal preco,
			java.math.BigDecimal custo) {
		var categoria = categoriaService.carregar(categoriaId);
		if (!categoria.isAtiva()) {
			throw new RegraDeNegocioException("Categoria inativa");
		}
		produto.setCategoria(categoria);
		produto.setNome(nome.trim());
		produto.setPrecoAtual(preco);
		produto.setCustoAtual(custo);
	}

	private Produto carregar(Long id) {
		Restaurante restaurante = escopo.restauranteDoContexto();
		Produto produto = produtoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));
		if (produto.getRestaurante() == null || !produto.getRestaurante().getId().equals(restaurante.getId())) {
			throw new RecursoNaoEncontradoException("Produto não encontrado");
		}
		return produto;
	}
}
