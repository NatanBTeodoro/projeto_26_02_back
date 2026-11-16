package com.projeto.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.TiDTO;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.Perfil;
import com.projeto.entity.enums.StatusRestaurante;
import com.projeto.repository.RestauranteRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RecursoNaoEncontradoException;
import com.projeto.util.exception.RegraDeNegocioException;

@Service
@Transactional(readOnly = true)
public class TiService {

	private final UsuarioRepository usuarioRepository;
	private final RestauranteRepository restauranteRepository;
	private final RestauranteService restauranteService;
	private final PasswordEncoder passwordEncoder;
	private final EscopoOperacao escopo;

	public TiService(
			UsuarioRepository usuarioRepository,
			RestauranteRepository restauranteRepository,
			RestauranteService restauranteService,
			PasswordEncoder passwordEncoder,
			EscopoOperacao escopo) {
		this.usuarioRepository = usuarioRepository;
		this.restauranteRepository = restauranteRepository;
		this.restauranteService = restauranteService;
		this.passwordEncoder = passwordEncoder;
		this.escopo = escopo;
	}

	public List<TiDTO.Admin> listar() {
		exigirTi();
		return usuarioRepository.findByPerfilOrderByNomeAsc(Perfil.ADMINISTRADOR).stream()
				.map(this::resposta)
				.toList();
	}

	@Transactional
	public TiDTO.Admin criar(TiDTO.Create dto) {
		exigirTi();
		String email = normalizar(dto.email());
		if (usuarioRepository.existsByEmailIgnoreCase(email)) {
			throw new RegraDeNegocioException("E-mail já cadastrado");
		}
		Usuario admin = new Usuario();
		admin.setNome(dto.nome().trim());
		admin.setEmail(email);
		admin.setSenhaHash(passwordEncoder.encode(dto.senha()));
		admin.setPerfil(Perfil.ADMINISTRADOR);
		admin.setAtivo(true);
		return resposta(usuarioRepository.save(admin));
	}

	@Transactional
	public TiDTO.Admin inativar(Long id) {
		exigirTi();
		Usuario admin = carregarAdmin(id);
		if (!admin.isAtivo()) {
			throw new RegraDeNegocioException("Admin já está inativo");
		}
		admin.setAtivo(false);
		admin.setTokenAcesso(null);
		for (Restaurante restaurante : restauranteRepository.findByAdminIdOrderByNomeAsc(id)) {
			if (restaurante.getStatus() == StatusRestaurante.ATIVO) {
				restaurante.setStatus(StatusRestaurante.INATIVO);
				restaurante.setInativadoPeloAdmin(true);
			}
		}
		for (Usuario pessoa : usuarioRepository.findDaEquipeDoAdmin(id)) {
			if (!pessoa.isAtivo()) {
				continue;
			}
			pessoa.setAtivo(false);
			pessoa.setTokenAcesso(null);
			pessoa.setInativadoPeloAdmin(true);
		}
		return resposta(usuarioRepository.save(admin));
	}

	@Transactional
	public TiDTO.Admin reativar(Long id) {
		exigirTi();
		Usuario admin = carregarAdmin(id);
		if (admin.isAtivo()) {
			throw new RegraDeNegocioException("Admin já está ativo");
		}
		admin.setAtivo(true);
		for (Restaurante restaurante : restauranteRepository.findByAdminIdOrderByNomeAsc(id)) {
			if (restaurante.isInativadoPeloAdmin() && restaurante.getStatus() == StatusRestaurante.INATIVO) {
				restaurante.setStatus(StatusRestaurante.ATIVO);
				restaurante.setInativadoPeloAdmin(false);
			}
		}
		for (Usuario pessoa : usuarioRepository.findDaEquipeDoAdmin(id)) {
			if (!pessoa.isInativadoPeloAdmin()) {
				continue;
			}
			boolean restauranteAtivo = pessoa.getRestaurante() != null
					&& pessoa.getRestaurante().getStatus() == StatusRestaurante.ATIVO;
			if (restauranteAtivo) {
				pessoa.setAtivo(true);
			}
			pessoa.setInativadoPeloAdmin(false);
		}
		return resposta(usuarioRepository.save(admin));
	}

	@Transactional
	public void deletar(Long id) {
		exigirTi();
		Usuario admin = carregarAdmin(id);
		restauranteService.apagarDoAdmin(admin);
		admin.setTokenAcesso(null);
		usuarioRepository.delete(admin);
	}

	private TiDTO.Admin resposta(Usuario admin) {
		List<TiDTO.RestauranteResumo> restaurantes = restauranteRepository.findByAdminIdOrderByNomeAsc(admin.getId())
				.stream()
				.map(restaurante -> new TiDTO.RestauranteResumo(
						restaurante.getId(), restaurante.getNome(), restaurante.getStatus()))
				.toList();
		return new TiDTO.Admin(admin.getId(), admin.getNome(), admin.getEmail(), admin.isAtivo(), restaurantes);
	}

	private Usuario carregarAdmin(Long id) {
		Usuario admin = usuarioRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Admin não encontrado"));
		if (admin.getPerfil() != Perfil.ADMINISTRADOR) {
			throw new RegraDeNegocioException("Conta não é de admin");
		}
		return admin;
	}

	private void exigirTi() {
		if (escopo.usuario().getPerfil() != Perfil.TI) {
			throw new AccessDeniedException("Acesso negado");
		}
	}

	private String normalizar(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
