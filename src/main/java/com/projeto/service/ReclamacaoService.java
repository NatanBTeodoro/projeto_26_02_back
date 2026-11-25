package com.projeto.service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.projeto.dto.ReclamacaoDTO;
import com.projeto.regra.AnexoReclamacao;
import com.projeto.regra.PeriodoReclamacao;
import com.projeto.entity.Reclamacao;
import com.projeto.entity.ReclamacaoAnexo;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.AssuntoGestor;
import com.projeto.entity.enums.DestinoReclamacao;
import com.projeto.entity.enums.Perfil;
import com.projeto.entity.enums.StatusReclamacao;
import com.projeto.repository.ReclamacaoAnexoRepository;
import com.projeto.repository.ReclamacaoRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RecursoNaoEncontradoException;
import com.projeto.util.exception.RegraDeNegocioException;

import jakarta.persistence.criteria.Predicate;

@Service
@Transactional(readOnly = true)
public class ReclamacaoService {

	private final ReclamacaoRepository reclamacaoRepository;
	private final ReclamacaoAnexoRepository anexoRepository;
	private final EscopoOperacao escopo;
	private final Autorizacao autorizacao;

	public ReclamacaoService(
			ReclamacaoRepository reclamacaoRepository,
			ReclamacaoAnexoRepository anexoRepository,
			EscopoOperacao escopo,
			Autorizacao autorizacao) {
		this.reclamacaoRepository = reclamacaoRepository;
		this.anexoRepository = anexoRepository;
		this.escopo = escopo;
		this.autorizacao = autorizacao;
	}

	public List<ReclamacaoDTO.Item> listar(StatusReclamacao status, LocalDate de, LocalDate ate, String caixa) {
		autorizacao.exigir(com.projeto.entity.enums.AuthorityFuncao.RECLAMACAO_LER);
		Usuario atual = escopo.usuario();
		Perfil perfil = atual.getPerfil();
		// vinculo, nao funcao: a caixa depende do destino, não da authority.
		if (perfil != Perfil.TI && perfil != Perfil.ADMINISTRADOR && perfil != Perfil.GESTOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		boolean recebidas = "recebidas".equals(caixa);
		if (caixa != null && !caixa.isBlank() && !recebidas && !"enviadas".equals(caixa)) {
			throw new RegraDeNegocioException("Caixa inválida");
		}
		// vinculo, nao funcao
		if (recebidas && perfil != Perfil.ADMINISTRADOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		PeriodoReclamacao.Limites limites = PeriodoReclamacao.de(de, ate);
		return reclamacaoRepository.findAll((raiz, consulta, cb) -> {
			if (consulta.getResultType() != Long.class && consulta.getResultType() != long.class) {
				consulta.orderBy(cb.desc(raiz.get("criadoEm")));
			}
			java.util.ArrayList<Predicate> filtros = new java.util.ArrayList<>();
			if (status != null) {
				filtros.add(cb.equal(raiz.get("status"), status));
			}
			filtros.add(cb.greaterThanOrEqualTo(raiz.get("criadoEm"), limites.inicio()));
			filtros.add(cb.lessThan(raiz.get("criadoEm"), limites.fim()));
			if (perfil == Perfil.TI) {
				filtros.add(cb.equal(raiz.get("destino"), DestinoReclamacao.TI));
			} else if (perfil == Perfil.GESTOR || !recebidas) {
				filtros.add(cb.equal(raiz.get("autor").get("id"), atual.getId()));
			} else {
				filtros.add(cb.equal(raiz.get("destino"), DestinoReclamacao.ADMINISTRADOR));
				filtros.add(cb.equal(raiz.get("adminDestino").get("id"), atual.getId()));
			}
			return cb.and(filtros.toArray(Predicate[]::new));
		}).stream().map(ReclamacaoDTO.Item::de).toList();
	}

	@Transactional
	public ReclamacaoDTO.Item criar(String assunto, String descricao, List<MultipartFile> arquivos) {
		autorizacao.exigir(com.projeto.entity.enums.AuthorityFuncao.RECLAMACAO_ABRIR);
		Usuario atual = escopo.usuario();
		// vinculo, nao funcao: gestor usa assunto fechado; admin escreve o assunto.
		if (atual.getPerfil() != Perfil.ADMINISTRADOR && atual.getPerfil() != Perfil.GESTOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		String texto = textoObrigatorio(descricao, "Informe a descrição", 4000);
		Reclamacao reclamacao = new Reclamacao();
		reclamacao.setAutor(atual);
		reclamacao.setAutorNome(atual.getNome());
		reclamacao.setAutorEmail(atual.getEmail());
		reclamacao.setCriadoEm(Instant.now());
		reclamacao.setDescricao(texto);
		reclamacao.setStatus(StatusReclamacao.ABERTA);
		// vinculo, nao funcao
		if (atual.getPerfil() == Perfil.GESTOR) {
			AssuntoGestor escolhido = AssuntoGestor.exigir(assunto);
			reclamacao.setAssunto(escolhido.rotulo());
			reclamacao.setDestino(escolhido.destino());
			Restaurante restaurante = atual.getRestaurante();
			if (restaurante != null) {
				reclamacao.setRestauranteNome(restaurante.getNome());
			}
			if (escolhido.destino() == DestinoReclamacao.ADMINISTRADOR) {
				if (restaurante == null || restaurante.getAdmin() == null) {
					throw new RegraDeNegocioException("Usuário sem restaurante");
				}
				reclamacao.setAdminDestino(restaurante.getAdmin());
			}
		} else {
			reclamacao.setAssunto(textoObrigatorio(assunto, "Informe o assunto", 150));
			reclamacao.setDestino(DestinoReclamacao.TI);
		}
		Reclamacao salva = reclamacaoRepository.save(reclamacao);
		guardarAnexo(salva, unico(arquivos));
		return ReclamacaoDTO.Item.de(salva);
	}

	@Transactional
	public ReclamacaoDTO.Item atualizar(Long id, ReclamacaoDTO.Atualizacao dto) {
		autorizacao.exigir(com.projeto.entity.enums.AuthorityFuncao.RECLAMACAO_RESPONDER);
		Usuario atual = escopo.usuario();
		Reclamacao reclamacao = carregar(id);
		if (!podeResponder(atual, reclamacao)) {
			throw new AccessDeniedException("Acesso negado");
		}
		reclamacao.setStatus(dto.status());
		if (dto.resposta() != null) {
			String resposta = dto.resposta().trim();
			if (resposta.length() > 4000) {
				throw new RegraDeNegocioException("A resposta passa de 4000 caracteres");
			}
			reclamacao.setResposta(resposta.isEmpty() ? null : resposta);
			reclamacao.setRespondidoEm(resposta.isEmpty() ? null : Instant.now());
		}
		return ReclamacaoDTO.Item.de(reclamacaoRepository.save(reclamacao));
	}

	public ReclamacaoDTO.Anexo baixar(Long id) {
		autorizacao.exigir(com.projeto.entity.enums.AuthorityFuncao.RECLAMACAO_LER);
		Reclamacao reclamacao = carregarVisivel(id);
		if (reclamacao.getAnexoNome() == null) {
			throw new RecursoNaoEncontradoException("Anexo não encontrado");
		}
		ReclamacaoAnexo anexo = anexoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Anexo não encontrado"));
		AnexoReclamacao.Arquivo seguro = AnexoReclamacao.paraDownload(reclamacao.getAnexoNome(), anexo.getConteudo());
		return new ReclamacaoDTO.Anexo(seguro.nome(), seguro.tipo(), anexo.getConteudo());
	}

	private MultipartFile unico(List<MultipartFile> arquivos) {
		if (arquivos == null || arquivos.isEmpty()) {
			return null;
		}
		List<MultipartFile> preenchidos = arquivos.stream()
				.filter(arquivo -> arquivo != null && !arquivo.isEmpty())
				.toList();
		if (preenchidos.size() > 1) {
			throw new RegraDeNegocioException(AnexoReclamacao.MENSAGEM_QUANTIDADE);
		}
		return preenchidos.isEmpty() ? null : preenchidos.get(0);
	}

	private void guardarAnexo(Reclamacao reclamacao, MultipartFile arquivo) {
		if (arquivo == null || arquivo.isEmpty()) {
			return;
		}
		if (arquivo.getSize() > AnexoReclamacao.LIMITE_BYTES) {
			throw new RegraDeNegocioException(AnexoReclamacao.MENSAGEM_TAMANHO);
		}
		byte[] conteudo;
		try {
			conteudo = arquivo.getBytes();
		} catch (IOException ex) {
			throw new RegraDeNegocioException("Não foi possível ler o anexo");
		}
		AnexoReclamacao.Arquivo seguro = AnexoReclamacao.validar(arquivo.getOriginalFilename(), conteudo);
		reclamacao.setAnexoNome(seguro.nome());
		ReclamacaoAnexo anexo = new ReclamacaoAnexo();
		anexo.setReclamacao(reclamacao);
		anexo.setTipo(seguro.tipo());
		anexo.setConteudo(conteudo);
		anexoRepository.save(anexo);
	}

	private Reclamacao carregarVisivel(Long id) {
		Usuario atual = escopo.usuario();
		Reclamacao reclamacao = carregar(id);
		// vinculo, nao funcao
		if (atual.getPerfil() == Perfil.TI && reclamacao.getDestino() == DestinoReclamacao.TI) {
			return reclamacao;
		}
		if (reclamacao.getAutor() != null && reclamacao.getAutor().getId().equals(atual.getId())) {
			return reclamacao;
		}
		if (podeResponder(atual, reclamacao)) {
			return reclamacao;
		}
		throw new AccessDeniedException("Acesso negado");
	}

	private boolean podeResponder(Usuario atual, Reclamacao reclamacao) {
		// vinculo, nao funcao: o destino da reclamação decide quem responde.
		if (atual.getPerfil() == Perfil.TI) {
			return reclamacao.getDestino() == DestinoReclamacao.TI;
		}
		if (atual.getPerfil() != Perfil.ADMINISTRADOR || reclamacao.getAdminDestino() == null) {
			return false;
		}
		return reclamacao.getAdminDestino().getId().equals(atual.getId());
	}

	private Reclamacao carregar(Long id) {
		return reclamacaoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Reclamação não encontrada"));
	}

	private String textoObrigatorio(String valor, String mensagem, int limite) {
		if (valor == null || valor.isBlank()) {
			throw new RegraDeNegocioException(mensagem);
		}
		String texto = valor.trim();
		if (texto.length() > limite) {
			throw new RegraDeNegocioException("O texto passa de " + limite + " caracteres");
		}
		return texto;
	}
}
