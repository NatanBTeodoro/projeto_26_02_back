package com.projeto.seguranca;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.Perfil;
import com.projeto.entity.enums.StatusRestaurante;
import com.projeto.repository.RestauranteRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.regra.RotuloAutor;
import com.projeto.util.exception.RecursoNaoEncontradoException;
import com.projeto.util.exception.RegraDeNegocioException;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class EscopoOperacao {

	public static final String CABECALHO_RESTAURANTE = "X-Restaurante-Id";

	private static final String USUARIO_NA_REQUISICAO = "escopo.usuario";

	private final HttpServletRequest request;
	private final SessaoAtual sessaoAtual;
	private final UsuarioRepository usuarioRepository;
	private final RestauranteRepository restauranteRepository;

	public EscopoOperacao(
			HttpServletRequest request,
			SessaoAtual sessaoAtual,
			UsuarioRepository usuarioRepository,
			RestauranteRepository restauranteRepository) {
		this.request = request;
		this.sessaoAtual = sessaoAtual;
		this.usuarioRepository = usuarioRepository;
		this.restauranteRepository = restauranteRepository;
	}

	public static Usuario administrador(Usuario atual) {
		if (atual.getPerfil() != Perfil.ADMINISTRADOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		return atual;
	}

	@Transactional(readOnly = true)
	public Usuario usuario() {
		Object salvo = request.getAttribute(USUARIO_NA_REQUISICAO);
		if (salvo instanceof Usuario usuario) {
			return usuario;
		}
		Long id = sessaoAtual.exigir().id();
		Usuario usuario = usuarioRepository.findParaEscopo(id)
				.orElseThrow(() -> new AccessDeniedException("Acesso negado"));
		request.setAttribute(USUARIO_NA_REQUISICAO, usuario);
		return usuario;
	}

	public Optional<Long> restauranteInformado() {
		String valor = request.getHeader(CABECALHO_RESTAURANTE);
		if (valor == null || valor.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.valueOf(valor.trim()));
		} catch (NumberFormatException ex) {
			throw new RegraDeNegocioException("Restaurante inválido");
		}
	}

	@Transactional(readOnly = true)
	public Restaurante restauranteDoContexto() {
		Usuario atual = usuario();
		if (atual.getPerfil() == Perfil.GESTOR || atual.getPerfil() == Perfil.USUARIO) {
			if (atual.getRestaurante() == null) {
				throw new RegraDeNegocioException("Usuário sem restaurante");
			}
			return atual.getRestaurante();
		}
		if (atual.getPerfil() == Perfil.ADMINISTRADOR) {
			Long id = restauranteInformado()
					.orElseThrow(() -> new RegraDeNegocioException("Abra a visão de gestor do restaurante"));
			return carregarDoAdmin(atual, id);
		}
		throw new AccessDeniedException("Acesso negado");
	}

	@Transactional(readOnly = true)
	public Restaurante exigirOperavel() {
		Restaurante restaurante = restauranteDoContexto();
		if (restaurante.getStatus() != StatusRestaurante.ATIVO) {
			throw new RegraDeNegocioException("Restaurante fechado ou inativo não pode ser alterado");
		}
		return restaurante;
	}

	@Transactional(readOnly = true)
	public Restaurante carregarDoAdmin(Usuario admin, Long restauranteId) {
		administrador(admin);
		Restaurante restaurante = restauranteRepository.findById(restauranteId)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Restaurante não encontrado"));
		if (!restaurante.getAdmin().getId().equals(admin.getId())) {
			throw new AccessDeniedException("Acesso negado");
		}
		return restaurante;
	}

	@Transactional(readOnly = true)
	public String autor() {
		Usuario atual = usuario();
		if (atual.getPerfil() == Perfil.ADMINISTRADOR && restauranteInformado().isPresent()) {
			return RotuloAutor.direto(atual.getPerfil(), atual.getNome(), atual.getEmail());
		}
		return RotuloAutor.assinatura(atual.getPerfil(), atual.getNome(), atual.getEmail());
	}
}
