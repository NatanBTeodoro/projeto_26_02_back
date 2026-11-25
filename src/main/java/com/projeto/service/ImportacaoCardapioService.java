package com.projeto.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.projeto.dto.ImportacaoCardapioDTO;
import com.projeto.planilha.ImportacaoCardapio;
import com.projeto.planilha.LeitorPlanilhaCardapio;
import com.projeto.entity.Categoria;
import com.projeto.entity.Produto;
import com.projeto.entity.Restaurante;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.OperacaoAuditoria;
import com.projeto.repository.CategoriaRepository;
import com.projeto.repository.ProdutoRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RegraDeNegocioException;

@Service
public class ImportacaoCardapioService {

	private final CategoriaRepository categoriaRepository;
	private final ProdutoRepository produtoRepository;
	private final EscopoOperacao escopo;
	private final AuditoriaFinanceiraService auditoria;
	private final Autorizacao autorizacao;

	public ImportacaoCardapioService(
			CategoriaRepository categoriaRepository,
			ProdutoRepository produtoRepository,
			EscopoOperacao escopo,
			AuditoriaFinanceiraService auditoria,
			Autorizacao autorizacao) {
		this.categoriaRepository = categoriaRepository;
		this.produtoRepository = produtoRepository;
		this.escopo = escopo;
		this.auditoria = auditoria;
		this.autorizacao = autorizacao;
	}

	public byte[] modelo() {
		// TODO(migracao): o modelo CSV continua sem CARDAPIO_LER para qualquer autenticado, como na fase 0.
		return LeitorPlanilhaCardapio.modeloCsv();
	}

	@Transactional(readOnly = true)
	public ImportacaoCardapioDTO.Previa prever(MultipartFile arquivo) {
		autorizacao.exigir(AuthorityFuncao.CARDAPIO_LER);
		return previa(arquivo);
	}

	@Transactional
	public ImportacaoCardapioDTO.Resultado confirmar(MultipartFile arquivo, ImportacaoCardapioDTO.Modo modo) {
		autorizacao.exigir(AuthorityFuncao.CARDAPIO_ESCREVER);
		Restaurante restaurante = escopo.exigirOperavel();
		ImportacaoCardapioDTO.Previa previa = previa(arquivo);
		if (!ImportacaoCardapio.deveAplicar(modo, previa.resumo().erros())) {
			return new ImportacaoCardapioDTO.Resultado(false, 0, previa.linhas(), previa.resumo());
		}
		Map<String, Categoria> categorias = new LinkedHashMap<>();
		for (Categoria categoria : categoriaRepository.findByRestauranteId(restaurante.getId())) {
			categorias.putIfAbsent(chave(categoria.getNome()), categoria);
		}
		Map<String, Produto> produtos = new LinkedHashMap<>();
		for (Produto produto : produtoRepository.findByRestauranteId(restaurante.getId())) {
			produtos.putIfAbsent(chave(produto.getCategoria().getNome()) + "|" + chave(produto.getNome()), produto);
		}

		int gravados = 0;
		List<Map<String, Object>> alteracoes = new ArrayList<>();
		for (ImportacaoCardapioDTO.Linha linha : previa.linhas()) {
			if (linha.situacao() == ImportacaoCardapioDTO.Situacao.ERRO) {
				continue;
			}
			Categoria categoria = categorias.get(chave(linha.categoria()));
			if (categoria == null) {
				categoria = new Categoria();
				categoria.setNome(linha.categoria());
				categoria.setAtiva(true);
				categoria.setRestaurante(restaurante);
				categoria = categoriaRepository.save(categoria);
				categorias.put(chave(categoria.getNome()), categoria);
			} else if (!categoria.isAtiva()) {
				categoria.setAtiva(true);
			}
			String chaveProduto = chave(linha.categoria()) + "|" + chave(linha.nome());
			Produto produto = produtos.get(chaveProduto);
			BigDecimal precoAntes = null;
			BigDecimal custoAntes = null;
			boolean criou = produto == null;
			if (produto == null) {
				produto = new Produto();
				produto.setCategoria(categoria);
				produto.setNome(linha.nome());
				produto.setAtivo(true);
				produto.setRestaurante(restaurante);
				produtos.put(chaveProduto, produto);
			} else {
				precoAntes = produto.getPrecoAtual();
				custoAntes = produto.getCustoAtual();
			}
			produto.setPrecoAtual(linha.preco());
			produto.setCustoAtual(linha.custo());
			Produto salvo = produtoRepository.save(produto);
			Map<String, Object> mudanca = new LinkedHashMap<>();
			mudanca.put("produtoId", salvo.getId());
			mudanca.put("nome", salvo.getNome());
			mudanca.put("criou", criou);
			mudanca.put("precoAntes", precoAntes);
			mudanca.put("custoAntes", custoAntes);
			mudanca.put("precoDepois", salvo.getPrecoAtual());
			mudanca.put("custoDepois", salvo.getCustoAtual());
			alteracoes.add(mudanca);
			gravados++;
		}
		List<Map<String, Object>> antes = new ArrayList<>();
		for (Map<String, Object> item : alteracoes) {
			Map<String, Object> linhaAntes = new LinkedHashMap<>();
			linhaAntes.put("produtoId", item.get("produtoId"));
			linhaAntes.put("preco", item.get("precoAntes"));
			linhaAntes.put("custo", item.get("custoAntes"));
			antes.add(linhaAntes);
		}
		auditoria.registrar(
				restaurante.getId(),
				escopo.usuario(),
				OperacaoAuditoria.IMPORTACAO_CONFIRMADA,
				restaurante.getId(),
				auditoria.json(antes),
				auditoria.json(alteracoes),
				"gravados=" + gravados);
		return new ImportacaoCardapioDTO.Resultado(true, gravados, previa.linhas(), previa.resumo());
	}

	private ImportacaoCardapioDTO.Previa previa(MultipartFile arquivo) {
		if (arquivo == null || arquivo.isEmpty()) {
			throw new RegraDeNegocioException("Envie um arquivo .csv ou .xlsx");
		}
		Restaurante restaurante = escopo.restauranteDoContexto();
		try {
			LeitorPlanilhaCardapio.Leitura leitura = LeitorPlanilhaCardapio.ler(arquivo.getOriginalFilename(), arquivo.getBytes());
			if (leitura.erro() != null) {
				throw new RegraDeNegocioException(leitura.erro());
			}
			return ImportacaoCardapio.prever(
					leitura.linhas(),
					categoriaRepository.findByRestauranteId(restaurante.getId()).stream()
							.map(categoria -> new ImportacaoCardapio.CategoriaConhecida(categoria.getNome(), categoria.isAtiva()))
							.toList(),
					produtoRepository.findByRestauranteId(restaurante.getId()).stream()
							.map(produto -> new ImportacaoCardapio.ProdutoConhecido(
									produto.getCategoria().getNome(),
									produto.getNome(),
									produto.getPrecoAtual(),
									produto.getCustoAtual(),
									produto.isAtivo()))
							.toList());
		} catch (RegraDeNegocioException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new RegraDeNegocioException("Não foi possível ler o arquivo");
		}
	}

	private static String chave(String texto) {
		return texto == null ? "" : texto.trim().toLowerCase(Locale.forLanguageTag("pt-BR"));
	}
}
