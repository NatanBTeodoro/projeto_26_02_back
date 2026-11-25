package com.projeto.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.ProdutoDTO;
import com.projeto.entity.Produto;
import com.projeto.entity.Restaurante;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.OperacaoAuditoria;
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
	private final AuditoriaFinanceiraService auditoria;
	private final Autorizacao autorizacao;

	public ProdutoService(
			ProdutoRepository produtoRepository,
			CategoriaService categoriaService,
			EscopoOperacao escopo,
			AuditoriaFinanceiraService auditoria,
			Autorizacao autorizacao) {
		this.produtoRepository = produtoRepository;
		this.categoriaService = categoriaService;
		this.escopo = escopo;
		this.auditoria = auditoria;
		this.autorizacao = autorizacao;
	}

	public List<ProdutoDTO.Response> listar() {
		autorizacao.exigir(AuthorityFuncao.CARDAPIO_LER);
		Restaurante restaurante = escopo.restauranteDoContexto();
		boolean custo = autorizacao.tem(AuthorityFuncao.CUSTO_LER);
		return produtoRepository.findByRestauranteIdOrderByNomeAsc(restaurante.getId()).stream()
				.map(produto -> ProdutoDTO.Response.de(produto, custo))
				.toList();
	}

	@Transactional
	public ProdutoDTO.Response criar(ProdutoDTO.Create dto) {
		autorizacao.exigir(AuthorityFuncao.CARDAPIO_ESCREVER);
		escopo.exigirOperavel();
		Produto produto = new Produto();
		BigDecimal custo = autorizacao.tem(AuthorityFuncao.CUSTO_LER) ? dto.custoAtual() : null;
		preencher(produto, dto.categoriaId(), dto.nome(), dto.precoAtual(), custo);
		produto.setRestaurante(produto.getCategoria().getRestaurante());
		produto.setAtivo(true);
		Produto salvo = produtoRepository.save(produto);
		auditar(OperacaoAuditoria.PRODUTO_CRIADO, salvo, null, null);
		return ProdutoDTO.Response.de(salvo, autorizacao.tem(AuthorityFuncao.CUSTO_LER));
	}

	@Transactional
	public ProdutoDTO.Response atualizar(Long id, ProdutoDTO.Update dto) {
		autorizacao.exigir(AuthorityFuncao.CARDAPIO_ESCREVER);
		escopo.exigirOperavel();
		Produto produto = carregar(id);
		BigDecimal precoAntes = produto.getPrecoAtual();
		BigDecimal custoAntes = produto.getCustoAtual();
		BigDecimal custo = autorizacao.tem(AuthorityFuncao.CUSTO_LER) ? dto.custoAtual() : produto.getCustoAtual();
		preencher(produto, dto.categoriaId(), dto.nome(), dto.precoAtual(), custo);
		produto.setRestaurante(produto.getCategoria().getRestaurante());
		if (dto.ativo() != null) {
			produto.setAtivo(dto.ativo());
		}
		Produto salvo = produtoRepository.save(produto);
		auditar(OperacaoAuditoria.PRODUTO_ALTERADO, salvo, precoAntes, custoAntes);
		return ProdutoDTO.Response.de(salvo, autorizacao.tem(AuthorityFuncao.CUSTO_LER));
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

	private void auditar(OperacaoAuditoria operacao, Produto produto, BigDecimal precoAntes, BigDecimal custoAntes) {
		Map<String, Object> antes = new LinkedHashMap<>();
		antes.put("precoAtual", precoAntes);
		antes.put("custoAtual", custoAntes);
		Map<String, Object> depois = new LinkedHashMap<>();
		depois.put("precoAtual", produto.getPrecoAtual());
		depois.put("custoAtual", produto.getCustoAtual());
		Long restauranteId = produto.getRestaurante() == null ? null : produto.getRestaurante().getId();
		auditoria.registrar(
				restauranteId,
				escopo.usuario(),
				operacao,
				produto.getId(),
				auditoria.json(antes),
				auditoria.json(depois),
				produto.getNome());
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
