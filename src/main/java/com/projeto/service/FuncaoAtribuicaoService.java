package com.projeto.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.FuncaoDTO;
import com.projeto.entity.AuditoriaPermissao;
import com.projeto.entity.FuncaoRede;
import com.projeto.entity.Usuario;
import com.projeto.entity.UsuarioFuncaoGlobal;
import com.projeto.entity.UsuarioRestauranteFuncao;
import com.projeto.entity.enums.AcaoPermissao;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.Perfil;
import com.projeto.repository.AuditoriaPermissaoRepository;
import com.projeto.repository.FuncaoRedeRepository;
import com.projeto.repository.UsuarioFuncaoGlobalRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.repository.UsuarioRestauranteFuncaoRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RecursoNaoEncontradoException;
import com.projeto.util.exception.RegraDeNegocioException;

@Service
public class FuncaoAtribuicaoService {

	private static final Set<AuthorityFuncao> BLOQUEADAS_PARA_GESTOR = EnumSet.of(
			AuthorityFuncao.EQUIPE_GESTOR_GERIR,
			AuthorityFuncao.PLATAFORMA_TI_GERIR,
			AuthorityFuncao.PLATAFORMA_ADMIN_GERIR);

	private final EscopoOperacao escopo;
	private final Autorizacao autorizacao;
	private final UsuarioRepository usuarios;
	private final UsuarioFuncaoGlobalRepository globais;
	private final UsuarioRestauranteFuncaoRepository doRestaurante;
	private final FuncaoRedeRepository catalogo;
	private final AuditoriaPermissaoRepository auditoria;
	private final FuncaoConsulta consulta;
	private final CargoRedeService cargos;

	public FuncaoAtribuicaoService(
			EscopoOperacao escopo,
			Autorizacao autorizacao,
			UsuarioRepository usuarios,
			UsuarioFuncaoGlobalRepository globais,
			UsuarioRestauranteFuncaoRepository doRestaurante,
			FuncaoRedeRepository catalogo,
			AuditoriaPermissaoRepository auditoria,
			FuncaoConsulta consulta,
			CargoRedeService cargos) {
		this.escopo = escopo;
		this.autorizacao = autorizacao;
		this.usuarios = usuarios;
		this.globais = globais;
		this.doRestaurante = doRestaurante;
		this.catalogo = catalogo;
		this.auditoria = auditoria;
		this.consulta = consulta;
		this.cargos = cargos;
	}

	@Transactional(readOnly = true)
	public List<AuthorityFuncao> minhas() {
		return consulta.doUsuario(escopo.usuario().getId());
	}

	@Transactional(readOnly = true)
	public List<FuncaoDTO.Linha> listarDoAlvo(Long alvoId) {
		autorizacao.exigir(AuthorityFuncao.EQUIPE_FUNCAO_ATRIBUIR);
		Usuario chamador = carregar(escopo.usuario().getId());
		Usuario alvo = carregar(alvoId);
		exigirMesmoRecorte(chamador, alvo, restauranteDo(alvo));
		if (alvo.getRestaurante() == null) {
			return globais.findByUsuarioId(alvoId).stream()
					.map(linha -> new FuncaoDTO.Linha(
							linha.getAuthority(), linha.getAtribuidoPor(), linha.getAtribuidoEm(), linha.getCargoOrigemId()))
					.toList();
		}
		return doRestaurante.findByUsuarioIdAndRestauranteId(alvoId, alvo.getRestaurante().getId()).stream()
				.map(linha -> new FuncaoDTO.Linha(
						linha.getAuthority(), linha.getAtribuidoPor(), linha.getAtribuidoEm(), linha.getCargoOrigemId()))
				.toList();
	}

	@Transactional
	public void aplicar(Long alvoId, FuncaoDTO.Atribuicao pedido) {
		autorizacao.exigir(AuthorityFuncao.EQUIPE_FUNCAO_ATRIBUIR);
		Usuario chamador = carregar(escopo.usuario().getId());
		Usuario alvo = carregar(alvoId);
		Long restauranteId = restauranteDo(alvo);
		if (pedido != null && pedido.restauranteId() != null && restauranteId != null
				&& !pedido.restauranteId().equals(restauranteId)) {
			throw new AccessDeniedException("Acesso negado");
		}
		exigirMesmoRecorte(chamador, alvo, restauranteId);
		if (pedido == null) {
			return;
		}
		if (pedido.conceder() != null) {
			for (AuthorityFuncao authority : pedido.conceder()) {
				conceder(chamador, alvo, restauranteId, authority);
			}
		}
		if (pedido.authorities() != null) {
			for (AuthorityFuncao authority : pedido.authorities()) {
				conceder(chamador, alvo, restauranteId, authority);
			}
		}
		if (pedido.cargos() != null) {
			for (Long cargoId : pedido.cargos()) {
				cargos.conceder(chamador, alvo, restauranteId, cargoId);
			}
		}
		if (pedido.revogar() != null) {
			for (AuthorityFuncao authority : pedido.revogar()) {
				revogar(chamador, alvo, restauranteId, authority);
			}
		}
		if (pedido.revogarAuthorities() != null) {
			for (AuthorityFuncao authority : pedido.revogarAuthorities()) {
				revogarAvulsa(chamador, alvo, restauranteId, authority);
			}
		}
		if (pedido.revogarCargos() != null) {
			for (Long cargoId : pedido.revogarCargos()) {
				cargos.revogar(chamador, alvo, restauranteId, cargoId);
			}
		}
	}

	@Transactional(readOnly = true)
	public List<FuncaoDTO.CatalogoItem> listarCatalogo() {
		exigirCatalogo();
		return catalogo.findByAdminIdAndAtivoTrueOrderByRotuloAsc(escopo.usuario().getId()).stream()
				.map(this::item)
				.toList();
	}

	@Transactional
	public List<FuncaoDTO.CatalogoItem> criarCatalogo(FuncaoDTO.CatalogoCriar pedido) {
		Usuario admin = exigirCatalogo();
		if (pedido == null || pedido.rotulo() == null || pedido.rotulo().isBlank()
				|| pedido.authorities() == null || pedido.authorities().isEmpty()) {
			throw new RegraDeNegocioException("Informe o rótulo e ao menos uma função");
		}
		for (AuthorityFuncao authority : pedido.authorities()) {
			barrarPlataforma(authority);
			autorizacao.exigirPodeConceder(admin, authority);
			if (catalogo.existsByAdminIdAndAuthority(admin.getId(), authority)) {
				throw new RegraDeNegocioException("Função já catalogada: " + authority.name());
			}
			FuncaoRede linha = new FuncaoRede();
			linha.setAdminId(admin.getId());
			linha.setAuthority(authority);
			linha.setRotulo(pedido.rotulo().trim());
			linha.setDescricao(pedido.descricao());
			linha.setAtivo(true);
			catalogo.save(linha);
		}
		return listarCatalogo();
	}

	@Transactional
	public FuncaoDTO.CatalogoItem atualizarCatalogo(Long id, FuncaoDTO.CatalogoAtualizar pedido) {
		Usuario admin = exigirCatalogo();
		FuncaoRede linha = catalogo.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Função não encontrada"));
		if (!linha.getAdminId().equals(admin.getId())) {
			throw new AccessDeniedException("Acesso negado");
		}
		if (pedido != null) {
			if (pedido.rotulo() != null && !pedido.rotulo().isBlank()) {
				linha.setRotulo(pedido.rotulo().trim());
			}
			if (pedido.descricao() != null) {
				linha.setDescricao(pedido.descricao());
			}
			if (pedido.ativo() != null) {
				linha.setAtivo(pedido.ativo());
			}
		}
		return item(catalogo.save(linha));
	}

	@Transactional(readOnly = true)
	public List<FuncaoDTO.AuditoriaItem> consultarAuditoria(Long alvoId, Long restauranteId, LocalDate de, LocalDate ate) {
		// vinculo, nao funcao
		if (escopo.usuario().getPerfil() != Perfil.ADMINISTRADOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		autorizacao.exigir(AuthorityFuncao.AUDITORIA_VER);
		Instant inicio = de == null ? Instant.EPOCH : de.atStartOfDay().toInstant(ZoneOffset.UTC);
		Instant fim = (ate == null ? LocalDate.now(ZoneOffset.UTC).plusDays(1) : ate.plusDays(1))
				.atStartOfDay()
				.toInstant(ZoneOffset.UTC);
		List<AuditoriaPermissao> linhas;
		if (alvoId != null) {
			linhas = auditoria.findByAlvoIdAndDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraDesc(alvoId, inicio, fim);
		} else if (restauranteId != null) {
			linhas = auditoria.findByRestauranteIdAndDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraDesc(
					restauranteId, inicio, fim);
		} else {
			linhas = auditoria.findByDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraDesc(inicio, fim);
		}
		return linhas.stream()
				.filter(linha -> restauranteId == null || restauranteId.equals(linha.getRestauranteId()))
				.filter(linha -> pertenceARede(linha.getAlvoId()))
				.map(linha -> new FuncaoDTO.AuditoriaItem(
						linha.getId(),
						linha.getDataHora(),
						linha.getAutorId(),
						linha.getAlvoId(),
						linha.getRestauranteId(),
						linha.getAuthority(),
						linha.getAcao()))
				.toList();
	}

	private void conceder(Usuario chamador, Usuario alvo, Long restauranteId, AuthorityFuncao authority) {
		barrar(chamador, authority);
		autorizacao.exigirPodeConceder(chamador, authority);
		if (restauranteId == null) {
			if (globais.existsByUsuarioIdAndAuthority(alvo.getId(), authority)) {
				return;
			}
			UsuarioFuncaoGlobal linha = new UsuarioFuncaoGlobal();
			linha.setUsuarioId(alvo.getId());
			linha.setAuthority(authority);
			linha.setAtribuidoPor(chamador.getId());
			linha.setAtribuidoEm(Instant.now());
			globais.save(linha);
		} else {
			if (doRestaurante.existsByUsuarioIdAndRestauranteIdAndAuthority(alvo.getId(), restauranteId, authority)) {
				return;
			}
			UsuarioRestauranteFuncao linha = new UsuarioRestauranteFuncao();
			linha.setUsuarioId(alvo.getId());
			linha.setRestauranteId(restauranteId);
			linha.setAuthority(authority);
			linha.setAtribuidoPor(chamador.getId());
			linha.setAtribuidoEm(Instant.now());
			doRestaurante.save(linha);
		}
		auditar(chamador, alvo, restauranteId, authority, AcaoPermissao.CONCEDER);
	}

	private void revogar(Usuario chamador, Usuario alvo, Long restauranteId, AuthorityFuncao authority) {
		barrar(chamador, authority);
		autorizacao.exigirPodeConceder(chamador, authority);
		if (restauranteId == null) {
			if (!globais.existsByUsuarioIdAndAuthority(alvo.getId(), authority)) {
				return;
			}
			globais.deleteByUsuarioIdAndAuthority(alvo.getId(), authority);
		} else {
			if (!doRestaurante.existsByUsuarioIdAndRestauranteIdAndAuthority(alvo.getId(), restauranteId, authority)) {
				return;
			}
			doRestaurante.deleteByUsuarioIdAndRestauranteIdAndAuthority(alvo.getId(), restauranteId, authority);
		}
		auditar(chamador, alvo, restauranteId, authority, AcaoPermissao.REVOGAR);
	}

	private void revogarAvulsa(Usuario chamador, Usuario alvo, Long restauranteId, AuthorityFuncao authority) {
		barrar(chamador, authority);
		autorizacao.exigirPodeConceder(chamador, authority);
		if (restauranteId == null) {
			for (com.projeto.entity.UsuarioFuncaoGlobal linha : globais.findByUsuarioId(alvo.getId())) {
				if (linha.getAuthority() == authority && linha.getCargoOrigemId() == null) {
					globais.delete(linha);
					auditar(chamador, alvo, null, authority, AcaoPermissao.REVOGAR);
				}
			}
			return;
		}
		for (com.projeto.entity.UsuarioRestauranteFuncao linha : doRestaurante.findByUsuarioIdAndRestauranteId(
				alvo.getId(), restauranteId)) {
			if (linha.getAuthority() == authority && linha.getCargoOrigemId() == null) {
				doRestaurante.delete(linha);
				auditar(chamador, alvo, restauranteId, authority, AcaoPermissao.REVOGAR);
			}
		}
	}

	private void barrar(Usuario chamador, AuthorityFuncao authority) {
		barrarPlataforma(authority);
		// vinculo, nao funcao
		if (chamador.getPerfil() == Perfil.GESTOR && BLOQUEADAS_PARA_GESTOR.contains(authority)) {
			throw new AccessDeniedException("Acesso negado");
		}
	}

	private void barrarPlataforma(AuthorityFuncao authority) {
		if (authority == AuthorityFuncao.PLATAFORMA_TI_GERIR || authority == AuthorityFuncao.PLATAFORMA_ADMIN_GERIR) {
			throw new AccessDeniedException("Acesso negado");
		}
	}

	private void exigirMesmoRecorte(Usuario chamador, Usuario alvo, Long restauranteId) {
		// vinculo, nao funcao
		if (chamador.getPerfil() == Perfil.GESTOR) {
			Long meu = restauranteDo(chamador);
			if (meu == null || restauranteId == null || !meu.equals(restauranteId)) {
				throw new AccessDeniedException("Acesso negado");
			}
			return;
		}
		if (chamador.getPerfil() == Perfil.ADMINISTRADOR) {
			if (alvo.getRestaurante() == null
					|| alvo.getRestaurante().getAdmin() == null
					|| !chamador.getId().equals(alvo.getRestaurante().getAdmin().getId())) {
				throw new AccessDeniedException("Acesso negado");
			}
			return;
		}
		throw new AccessDeniedException("Acesso negado");
	}

	private Usuario exigirCatalogo() {
		// vinculo, nao funcao
		if (escopo.usuario().getPerfil() != Perfil.ADMINISTRADOR) {
			throw new AccessDeniedException("Acesso negado");
		}
		autorizacao.exigir(AuthorityFuncao.CATALOGO_FUNCOES_GERIR);
		return carregar(escopo.usuario().getId());
	}

	private boolean pertenceARede(Long alvoId) {
		return usuarios.findParaEscopo(alvoId)
				.filter(alvo -> alvo.getRestaurante() != null && alvo.getRestaurante().getAdmin() != null)
				.map(alvo -> escopo.usuario().getId().equals(alvo.getRestaurante().getAdmin().getId()))
				.orElse(false);
	}

	private void auditar(Usuario chamador, Usuario alvo, Long restauranteId, AuthorityFuncao authority, AcaoPermissao acao) {
		AuditoriaPermissao registro = new AuditoriaPermissao();
		registro.setDataHora(Instant.now());
		registro.setAutorId(chamador.getId());
		registro.setAlvoId(alvo.getId());
		registro.setRestauranteId(restauranteId);
		registro.setAuthority(authority.name());
		registro.setAcao(acao);
		auditoria.save(registro);
	}

	private Usuario carregar(Long id) {
		return usuarios.findParaEscopo(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
	}

	private Long restauranteDo(Usuario usuario) {
		return usuario.getRestaurante() == null ? null : usuario.getRestaurante().getId();
	}

	private FuncaoDTO.CatalogoItem item(FuncaoRede linha) {
		return new FuncaoDTO.CatalogoItem(
				linha.getId(), linha.getRotulo(), linha.getDescricao(), linha.getAuthority(), linha.isAtivo());
	}
}
