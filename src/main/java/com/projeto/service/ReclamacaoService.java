package com.projeto.service;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.projeto.dto.ReclamacaoDTO;
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

	public ReclamacaoService(
			ReclamacaoRepository reclamacaoRepository,
			ReclamacaoAnexoRepository anexoRepository,
			EscopoOperacao escopo) {
		this.reclamacaoRepository = reclamacaoRepository;
		this.anexoRepository = anexoRepository;
		this.escopo = escopo;
	}

	public List<ReclamacaoDTO.Item> listar(StatusReclamacao status, LocalDate de, LocalDate ate, String caixa) {
		Usuario atual = escopo.usuario();
		Perfil perfil = atual.getPerfil();
		if (perfil != Perfil.TI && perfil != Perfil.ADMINISTRADOR && perfil != Perfil.GESTOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		boolean recebidas = "recebidas".equals(caixa);
		if (caixa != null && !caixa.isBlank() && !recebidas && !"enviadas".equals(caixa)) {
			throw new RegraDeNegocioException("Caixa inválida");
		}
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
	public ReclamacaoDTO.Item criar(String assunto, String descricao, MultipartFile arquivo) {
		Usuario atual = escopo.usuario();
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
		guardarAnexo(salva, arquivo);
		return ReclamacaoDTO.Item.de(salva);
	}

	@Transactional
	public ReclamacaoDTO.Item atualizar(Long id, ReclamacaoDTO.Atualizacao dto) {
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
		Reclamacao reclamacao = carregarVisivel(id);
		if (reclamacao.getAnexoNome() == null) {
			throw new RecursoNaoEncontradoException("Anexo não encontrado");
		}
		ReclamacaoAnexo anexo = anexoRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Anexo não encontrado"));
		return new ReclamacaoDTO.Anexo(reclamacao.getAnexoNome(), anexo.getTipo(), anexo.getConteudo());
	}

	private void guardarAnexo(Reclamacao reclamacao, MultipartFile arquivo) {
		if (arquivo == null || arquivo.isEmpty()) {
			return;
		}
		String nome = arquivo.getOriginalFilename();
		if (nome == null || nome.isBlank()) {
			nome = "anexo";
		}
		nome = Paths.get(nome).getFileName().toString().replace("\"", "");
		if (nome.isBlank()) {
			nome = "anexo";
		}
		if (nome.length() > 255) {
			nome = nome.substring(nome.length() - 255);
		}
		String tipo = arquivo.getContentType();
		if (tipo != null && tipo.length() > 120) {
			tipo = tipo.substring(0, 120);
		}
		byte[] conteudo;
		try {
			conteudo = arquivo.getBytes();
		} catch (IOException ex) {
			throw new RegraDeNegocioException("Não foi possível ler o anexo");
		}
		reclamacao.setAnexoNome(nome);
		ReclamacaoAnexo anexo = new ReclamacaoAnexo();
		anexo.setReclamacao(reclamacao);
		anexo.setTipo(tipo);
		anexo.setConteudo(conteudo);
		anexoRepository.save(anexo);
	}

	private Reclamacao carregarVisivel(Long id) {
		Usuario atual = escopo.usuario();
		Reclamacao reclamacao = carregar(id);
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
