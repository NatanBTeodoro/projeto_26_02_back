package com.projeto.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.ComandaDTO;
import com.projeto.entity.Comanda;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.Perfil;
import com.projeto.repository.ComandaRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RecursoNaoEncontradoException;
import com.projeto.util.exception.RegraDeNegocioException;

@Service
@Transactional(readOnly = true)
public class ComandaService {

	private final ComandaRepository comandaRepository;
	private final UsuarioRepository usuarioRepository;
	private final EscopoOperacao escopo;

	public ComandaService(
			ComandaRepository comandaRepository,
			UsuarioRepository usuarioRepository,
			EscopoOperacao escopo) {
		this.comandaRepository = comandaRepository;
		this.usuarioRepository = usuarioRepository;
		this.escopo = escopo;
	}

	public List<ComandaDTO.Response> listar(Long usuarioId) {
		Usuario atual = escopo.usuario();
		if (atual.getPerfil() == Perfil.USUARIO) {
			return comandaRepository.findByUsuarioIdOrderByIdDesc(atual.getId()).stream()
					.map(ComandaDTO.Response::de)
					.toList();
		}
		Restaurante restaurante = escopo.restauranteDoContexto();
		if (usuarioId == null) {
			throw new RegraDeNegocioException("Informe o usuário");
		}
		Usuario alvo = usuarioDoRestaurante(restaurante, usuarioId);
		return comandaRepository.findByRestauranteIdAndUsuarioIdOrderByIdDesc(restaurante.getId(), alvo.getId())
				.stream()
				.map(ComandaDTO.Response::de)
				.toList();
	}

	@Transactional
	public ComandaDTO.Response abrir(ComandaDTO.Create dto) {
		Usuario atual = escopo.usuario();
		Restaurante restaurante = escopo.exigirOperavel();
		Usuario dono;
		if (atual.getPerfil() == Perfil.USUARIO) {
			dono = atual;
		} else if (atual.getPerfil() == Perfil.GESTOR || atual.getPerfil() == Perfil.ADMINISTRADOR) {
			if (dto == null || dto.usuarioId() == null) {
				throw new RegraDeNegocioException("Informe o usuário");
			}
			dono = usuarioDoRestaurante(restaurante, dto.usuarioId());
		} else {
			throw new AccessDeniedException("Acesso negado");
		}
		Comanda comanda = new Comanda();
		comanda.setRestaurante(restaurante);
		comanda.setUsuario(dono);
		comanda.setCriadaEm(Instant.now());
		comanda.setAberta(true);
		return ComandaDTO.Response.de(comandaRepository.save(comanda));
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
