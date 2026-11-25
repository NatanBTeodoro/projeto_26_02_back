package com.projeto.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.SenhaDTO;
import com.projeto.dto.UsuarioDTO;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.Perfil;
import com.projeto.entity.enums.StatusRestaurante;
import com.projeto.repository.ComandaRepository;
import com.projeto.repository.PedidoRepository;
import com.projeto.repository.RestauranteRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.Emails;
import com.projeto.util.exception.RecursoNaoEncontradoException;
import com.projeto.util.exception.RegraDeNegocioException;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;
	private final RestauranteRepository restauranteRepository;
	private final PedidoRepository pedidoRepository;
	private final ComandaRepository comandaRepository;
	private final PasswordEncoder passwordEncoder;
	private final EscopoOperacao escopo;
	private final FuncaoPadrao funcaoPadrao;
	private final CargoRedeService cargoRede;
	private final Autorizacao autorizacao;
	private final String emailPrincipal;

	public UsuarioService(
			UsuarioRepository usuarioRepository,
			RestauranteRepository restauranteRepository,
			PedidoRepository pedidoRepository,
			ComandaRepository comandaRepository,
			PasswordEncoder passwordEncoder,
			EscopoOperacao escopo,
			FuncaoPadrao funcaoPadrao,
			CargoRedeService cargoRede,
			Autorizacao autorizacao,
			@Value("${app.semente.email}") String emailPrincipal) {
		this.usuarioRepository = usuarioRepository;
		this.restauranteRepository = restauranteRepository;
		this.pedidoRepository = pedidoRepository;
		this.comandaRepository = comandaRepository;
		this.passwordEncoder = passwordEncoder;
		this.escopo = escopo;
		this.funcaoPadrao = funcaoPadrao;
		this.cargoRede = cargoRede;
		this.autorizacao = autorizacao;
		this.emailPrincipal = emailPrincipal;
	}

	public List<UsuarioDTO.Response> listar(Long restauranteId) {
		autorizacao.exigir(AuthorityFuncao.EQUIPE_LER);
		Usuario atual = escopo.usuario();
		// vinculo, nao funcao
		if (atual.getPerfil() == Perfil.GESTOR) {
			if (atual.getRestaurante() == null) {
				throw new RegraDeNegocioException("Usuário sem restaurante");
			}
			return mapear(usuarioRepository.findByRestauranteIdAndPerfilOrderByNomeAsc(
					atual.getRestaurante().getId(), Perfil.USUARIO));
		}
		// vinculo, nao funcao
		EscopoOperacao.administrador(atual);
		if (restauranteId != null) {
			Restaurante restaurante = escopo.carregarDoAdmin(atual, restauranteId);
			return mapear(usuarioRepository.findByRestauranteIdOrderByNomeAsc(restaurante.getId()));
		}
		if (escopo.restauranteInformado().isPresent()) {
			Restaurante restaurante = escopo.restauranteDoContexto();
			return mapear(usuarioRepository.findByRestauranteIdOrderByNomeAsc(restaurante.getId()));
		}
		return mapear(usuarioRepository.findDaEquipeDoAdmin(atual.getId()));
	}

	@Transactional
	public UsuarioDTO.Response criar(UsuarioDTO.Create dto) {
		Usuario atual = escopo.usuario();
		String email = Emails.normalizar(dto.email());
		if (usuarioRepository.existsByEmailIgnoreCase(email)) {
			throw new RegraDeNegocioException("E-mail já cadastrado");
		}
		Restaurante restaurante = restauranteDoCadastro(atual, dto);
		Perfil perfil = perfilDoCadastro(dto.perfil());
		if (restaurante.getStatus() != StatusRestaurante.ATIVO) {
			throw new RegraDeNegocioException("Restaurante fechado ou inativo não pode ser alterado");
		}
		Usuario usuario = new Usuario();
		usuario.setNome(nomeLivre(dto.nome().trim(), restaurante.getId(), null));
		usuario.setEmail(email);
		usuario.setSenhaHash(passwordEncoder.encode(dto.senha()));
		usuario.setPerfil(perfil);
		usuario.setRestaurante(restaurante);
		usuario.setAtivo(true);
		Usuario salvo = usuarioRepository.save(usuario);
		funcaoPadrao.aplicar(salvo);
		cargoRede.vincularPacote(salvo);
		return UsuarioDTO.Response.de(salvo);
	}

	@Transactional
	public UsuarioDTO.Response atualizar(Long id, UsuarioDTO.Update dto) {
		Perfil perfilNovo = null;
		if (dto.perfil() != null) {
			Usuario candidato = usuarioRepository.findById(id)
					.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
			if (candidato.getPerfil() != dto.perfil()) {
				ContaPrincipal.exigirPodeRebaixar(candidato.getEmail(), emailPrincipal, dto.perfil());
				Usuario atual = escopo.usuario();
				if (dto.perfil() == Perfil.GESTOR) {
					autorizacao.exigir(AuthorityFuncao.EQUIPE_GESTOR_GERIR);
				}
				if (dto.perfil() == Perfil.USUARIO) {
					autorizacao.exigir(AuthorityFuncao.EQUIPE_USUARIO_GERIR);
				}
				TrocaPerfil.validar(atual.getPerfil(), atual.getId(), candidato.getId(), candidato.getPerfil(), dto.perfil());
				perfilNovo = dto.perfil();
			}
		}
		Usuario usuario = carregarGerivel(id);
		if (dto.nome() != null && !dto.nome().isBlank()) {
			Long restauranteId = usuario.getRestaurante() == null ? null : usuario.getRestaurante().getId();
			usuario.setNome(nomeLivre(dto.nome().trim(), restauranteId, usuario.getNome()));
		}
		if (dto.email() != null && !dto.email().isBlank()) {
			String email = Emails.normalizar(dto.email());
			usuarioRepository.findByEmailIgnoreCase(email)
					.filter(outro -> !outro.getId().equals(usuario.getId()))
					.ifPresent(outro -> {
						throw new RegraDeNegocioException("E-mail já cadastrado");
					});
			usuario.setEmail(email);
		}
		if (dto.ativo() != null) {
			if (!dto.ativo() && usuario.getId().equals(escopo.usuario().getId())) {
				throw new RegraDeNegocioException("Não é possível inativar o próprio usuário");
			}
			if (dto.ativo()) {
				if (usuario.getRestaurante() == null
						|| usuario.getRestaurante().getStatus() != StatusRestaurante.ATIVO) {
					throw new RegraDeNegocioException("Restaurante fechado ou inativo não pode ser alterado");
				}
				usuario.setAtivo(true);
				usuario.setInativadoPeloAdmin(false);
				usuario.setInativadoPeloRestaurante(false);
			} else {
				usuario.setAtivo(false);
				usuario.setTokenAcesso(null);
				usuario.setInativadoPeloAdmin(false);
				usuario.setInativadoPeloRestaurante(false);
			}
		}
		if (perfilNovo != null) {
			usuario.setPerfil(perfilNovo);
			usuario.setTokenAcesso(null);
		}
		return UsuarioDTO.Response.de(usuarioRepository.save(usuario));
	}

	@Transactional
	public void deletar(Long id) {
		usuarioRepository.findById(id).ifPresent(candidato -> ContaPrincipal.exigirPodeRemover(candidato.getEmail(), emailPrincipal));
		Usuario usuario = carregarGerivel(id);
		if (usuario.getId().equals(escopo.usuario().getId())) {
			throw new RegraDeNegocioException("Não é possível deletar o próprio usuário");
		}
		pedidoRepository.desvincularUsuario(id);
		comandaRepository.desvincularUsuario(id);
		usuario.setTokenAcesso(null);
		usuarioRepository.delete(usuario);
	}

	@Transactional
	public void redefinirSenha(Long id, SenhaDTO.Redefinicao dto) {
		autorizacao.exigir(AuthorityFuncao.EQUIPE_SENHA_REDEFINIR);
		Usuario atual = escopo.usuario();
		Usuario alvo = usuarioRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
		if (alvo.getId().equals(atual.getId())) {
			throw new RegraDeNegocioException("Use a troca de senha do perfil, com a senha atual");
		}
		// vinculo, nao funcao
		if (atual.getPerfil() == Perfil.ADMINISTRADOR) {
			if (alvo.getPerfil() != Perfil.GESTOR && alvo.getPerfil() != Perfil.USUARIO) {
				throw new AccessDeniedException("Acesso negado");
			}
			if (alvo.getRestaurante() == null || !alvo.getRestaurante().getAdmin().getId().equals(atual.getId())) {
				throw new AccessDeniedException("Acesso negado");
			}
		} else if (atual.getPerfil() == Perfil.GESTOR) {
			if (alvo.getPerfil() != Perfil.USUARIO) {
				throw new RegraDeNegocioException("O gestor só redefine senha de usuário");
			}
			if (atual.getRestaurante() == null || alvo.getRestaurante() == null
					|| !atual.getRestaurante().getId().equals(alvo.getRestaurante().getId())) {
				throw new AccessDeniedException("Acesso negado");
			}
		} else {
			throw new AccessDeniedException("Acesso negado");
		}
		alvo.setSenhaHash(passwordEncoder.encode(dto.senhaNova()));
		alvo.setTokenAcesso(null);
		usuarioRepository.save(alvo);
	}

	private Restaurante restauranteDoCadastro(Usuario atual, UsuarioDTO.Create dto) {
		// vinculo, nao funcao
		if (atual.getPerfil() == Perfil.GESTOR) {
			if (atual.getRestaurante() == null) {
				throw new RegraDeNegocioException("Usuário sem restaurante");
			}
			return atual.getRestaurante();
		}
		// vinculo, nao funcao
		if (atual.getPerfil() == Perfil.ADMINISTRADOR) {
			if (escopo.restauranteInformado().isPresent()) {
				return escopo.restauranteDoContexto();
			}
			if (dto.restauranteId() == null) {
				throw new RegraDeNegocioException("Informe o restaurante");
			}
			return restauranteRepository.findById(dto.restauranteId())
					.filter(restaurante -> restaurante.getAdmin().getId().equals(atual.getId()))
					.orElseThrow(() -> new RecursoNaoEncontradoException("Restaurante não encontrado"));
		}
		throw new AccessDeniedException("Acesso negado");
	}

	private Perfil perfilDoCadastro(Perfil perfil) {
		if (perfil == Perfil.USUARIO) {
			autorizacao.exigir(AuthorityFuncao.EQUIPE_USUARIO_GERIR);
			return Perfil.USUARIO;
		}
		if (perfil == Perfil.GESTOR) {
			autorizacao.exigir(AuthorityFuncao.EQUIPE_GESTOR_GERIR);
			return Perfil.GESTOR;
		}
		throw new RegraDeNegocioException("Perfil inválido");
	}

	private Usuario carregarGerivel(Long id) {
		Usuario atual = escopo.usuario();
		Usuario usuario = usuarioRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
		if (usuario.getPerfil() == Perfil.USUARIO) {
			autorizacao.exigir(AuthorityFuncao.EQUIPE_USUARIO_GERIR);
		} else if (usuario.getPerfil() == Perfil.GESTOR) {
			autorizacao.exigir(AuthorityFuncao.EQUIPE_GESTOR_GERIR);
		} else {
			throw new AccessDeniedException("Acesso negado");
		}
		// vinculo, nao funcao
		if (atual.getPerfil() == Perfil.GESTOR) {
			if (usuario.getPerfil() != Perfil.USUARIO
					|| atual.getRestaurante() == null
					|| usuario.getRestaurante() == null
					|| !atual.getRestaurante().getId().equals(usuario.getRestaurante().getId())) {
				throw new AccessDeniedException("Acesso negado");
			}
			return usuario;
		}
		if (atual.getPerfil() == Perfil.ADMINISTRADOR) {
			if ((usuario.getPerfil() != Perfil.GESTOR && usuario.getPerfil() != Perfil.USUARIO)
					|| usuario.getRestaurante() == null
					|| !usuario.getRestaurante().getAdmin().getId().equals(atual.getId())) {
				throw new AccessDeniedException("Acesso negado");
			}
			return usuario;
		}
		throw new AccessDeniedException("Acesso negado");
	}

	private List<UsuarioDTO.Response> mapear(List<Usuario> usuarios) {
		return usuarios.stream().map(UsuarioDTO.Response::de).toList();
	}

	private String nomeLivre(String digitado, Long restauranteId, String nomeAtual) {
		java.util.List<String> nomes = restauranteId == null
				? usuarioRepository.nomesSemRestaurante()
				: usuarioRepository.nomesDoRestaurante(restauranteId);
		if (nomeAtual != null) {
			nomes = nomes.stream().filter(nome -> !nome.equalsIgnoreCase(nomeAtual)).toList();
		}
		return NomeDuplicado.reservar(digitado, nomes);
	}
}
