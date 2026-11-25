package com.projeto.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.FuncaoDTO;
import com.projeto.entity.AuditoriaPermissao;
import com.projeto.entity.CargoRede;
import com.projeto.entity.CargoRedeAuthority;
import com.projeto.entity.Usuario;
import com.projeto.entity.UsuarioFuncaoGlobal;
import com.projeto.entity.UsuarioRestauranteFuncao;
import com.projeto.entity.enums.AcaoPermissao;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.Perfil;
import com.projeto.repository.AuditoriaPermissaoRepository;
import com.projeto.repository.CargoRedeAuthorityRepository;
import com.projeto.repository.CargoRedeRepository;
import com.projeto.repository.UsuarioFuncaoGlobalRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.repository.UsuarioRestauranteFuncaoRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RecursoNaoEncontradoException;
import com.projeto.util.exception.RegraDeNegocioException;

@Service
public class CargoRedeService {

	private final EscopoOperacao escopo;
	private final Autorizacao autorizacao;
	private final UsuarioRepository usuarios;
	private final CargoRedeRepository cargos;
	private final CargoRedeAuthorityRepository itens;
	private final UsuarioRestauranteFuncaoRepository doRestaurante;
	private final UsuarioFuncaoGlobalRepository globais;
	private final AuditoriaPermissaoRepository auditoria;

	public CargoRedeService(
			EscopoOperacao escopo,
			Autorizacao autorizacao,
			UsuarioRepository usuarios,
			CargoRedeRepository cargos,
			CargoRedeAuthorityRepository itens,
			UsuarioRestauranteFuncaoRepository doRestaurante,
			UsuarioFuncaoGlobalRepository globais,
			AuditoriaPermissaoRepository auditoria) {
		this.escopo = escopo;
		this.autorizacao = autorizacao;
		this.usuarios = usuarios;
		this.cargos = cargos;
		this.itens = itens;
		this.doRestaurante = doRestaurante;
		this.globais = globais;
		this.auditoria = auditoria;
	}

	@Transactional
	public void garantirPadrao(Long adminId) {
		if (adminId == null) {
			return;
		}
		for (CargosPadrao.Definicao definicao : CargosPadrao.todos()) {
			if (cargos.findByAdminIdAndNome(adminId, definicao.nome()).isPresent()) {
				continue;
			}
			CargoRede cargo = new CargoRede();
			cargo.setAdminId(adminId);
			cargo.setNome(definicao.nome());
			cargo.setDescricao(definicao.descricao());
			cargo.setAtivo(true);
			CargoRede salvo = cargos.save(cargo);
			for (AuthorityFuncao authority : definicao.authorities()) {
				gravarItem(salvo.getId(), authority);
			}
		}
	}

	@Transactional
	public void vincularPacote(Usuario usuario) {
		if (usuario == null || usuario.getRestaurante() == null || usuario.getRestaurante().getAdmin() == null) {
			return;
		}
		String nome = usuario.getPerfil() == Perfil.GESTOR
				? "Gerente"
				: usuario.getPerfil() == Perfil.USUARIO ? "Atendente" : null;
		if (nome == null) {
			return;
		}
		Long adminId = usuario.getRestaurante().getAdmin().getId();
		garantirPadrao(adminId);
		CargoRede cargo = cargos.findByAdminIdAndNome(adminId, nome).orElse(null);
		if (cargo == null) {
			return;
		}
		Set<AuthorityFuncao> doCargo = authorities(cargo.getId());
		Long restauranteId = usuario.getRestaurante().getId();
		for (UsuarioRestauranteFuncao linha : doRestaurante.findByUsuarioIdAndRestauranteId(usuario.getId(), restauranteId)) {
			if (linha.getCargoOrigemId() == null && doCargo.contains(linha.getAuthority())) {
				linha.setCargoOrigemId(cargo.getId());
				doRestaurante.save(linha);
			}
		}
	}

	@Transactional(readOnly = true)
	public List<FuncaoDTO.CargoItem> listarDaRede() {
		Usuario admin = exigirCatalogo();
		return montar(cargos.findByAdminIdOrderByNomeAsc(admin.getId()));
	}

	@Transactional(readOnly = true)
	public List<FuncaoDTO.CargoItem> atribuiveis() {
		autorizacao.exigir(AuthorityFuncao.EQUIPE_FUNCAO_ATRIBUIR);
		Usuario chamador = carregar(escopo.usuario().getId());
		Long adminId = adminDaRede(chamador);
		if (adminId == null) {
			return List.of();
		}
		List<FuncaoDTO.CargoItem> saida = new ArrayList<>();
		for (FuncaoDTO.CargoItem cargo : montar(cargos.findByAdminIdAndAtivoTrueOrderByNomeAsc(adminId))) {
			if (podeAtribuir(chamador, cargo.authorities())) {
				saida.add(cargo);
			}
		}
		return saida;
	}

	@Transactional
	public FuncaoDTO.CargoItem criar(FuncaoDTO.CargoCriar pedido) {
		Usuario admin = exigirCatalogo();
		if (pedido == null || pedido.nome() == null || pedido.nome().isBlank()) {
			throw new RegraDeNegocioException("Informe o nome do cargo");
		}
		if (cargos.findByAdminIdAndNome(admin.getId(), pedido.nome().trim()).isPresent()) {
			throw new RegraDeNegocioException("Já existe um cargo com esse nome");
		}
		Set<AuthorityFuncao> authorities = validar(admin, pedido.authorities());
		CargoRede cargo = new CargoRede();
		cargo.setAdminId(admin.getId());
		cargo.setNome(pedido.nome().trim());
		cargo.setDescricao(pedido.descricao());
		cargo.setAtivo(true);
		CargoRede salvo = cargos.save(cargo);
		for (AuthorityFuncao authority : authorities) {
			gravarItem(salvo.getId(), authority);
		}
		return item(salvo, authorities);
	}

	@Transactional
	public FuncaoDTO.CargoItem atualizar(Long id, FuncaoDTO.CargoAtualizar pedido) {
		Usuario admin = exigirCatalogo();
		CargoRede cargo = carregarCargo(id);
		if (!admin.getId().equals(cargo.getAdminId())) {
			throw new AccessDeniedException("Acesso negado");
		}
		if (pedido == null) {
			return item(cargo, authorities(cargo.getId()));
		}
		if (pedido.nome() != null && !pedido.nome().isBlank()) {
			cargo.setNome(pedido.nome().trim());
		}
		if (pedido.descricao() != null) {
			cargo.setDescricao(pedido.descricao());
		}
		if (pedido.ativo() != null) {
			cargo.setAtivo(pedido.ativo());
		}
		cargos.save(cargo);
		if (pedido.authorities() != null) {
			Set<AuthorityFuncao> novas = validar(admin, pedido.authorities());
			Set<AuthorityFuncao> atuais = authorities(cargo.getId());
			Set<AuthorityFuncao> adicionadas = EnumSet.noneOf(AuthorityFuncao.class);
			Set<AuthorityFuncao> removidas = EnumSet.noneOf(AuthorityFuncao.class);
			for (AuthorityFuncao authority : novas) {
				if (!atuais.contains(authority)) {
					gravarItem(cargo.getId(), authority);
					adicionadas.add(authority);
				}
			}
			for (AuthorityFuncao authority : atuais) {
				if (!novas.contains(authority)) {
					itens.deleteByCargoRedeIdAndAuthority(cargo.getId(), authority);
					removidas.add(authority);
				}
			}
			propagar(cargo.getId(), adicionadas, removidas);
			return item(cargo, novas);
		}
		return item(cargo, authorities(cargo.getId()));
	}

	@Transactional
	public void conceder(Usuario chamador, Usuario alvo, Long restauranteId, Long cargoId) {
		CargoRede cargo = carregarCargo(cargoId);
		exigirCargoDaRede(chamador, cargo);
		Set<AuthorityFuncao> authorities = authorities(cargo.getId());
		if (!podeAtribuir(chamador, authorities)) {
			throw new AccessDeniedException("Acesso negado");
		}
		for (AuthorityFuncao authority : authorities) {
			concederAuthority(chamador, alvo, restauranteId, authority, cargo.getId(), AcaoPermissao.CONCEDER);
		}
	}

	@Transactional
	public void revogar(Usuario chamador, Usuario alvo, Long restauranteId, Long cargoId) {
		carregarCargo(cargoId);
		if (restauranteId == null) {
			for (UsuarioFuncaoGlobal linha : globais.findByUsuarioIdAndCargoOrigemId(alvo.getId(), cargoId)) {
				globais.delete(linha);
				auditar(chamador, alvo, null, linha.getAuthority(), AcaoPermissao.REVOGAR);
			}
			return;
		}
		for (UsuarioRestauranteFuncao linha : doRestaurante.findByUsuarioIdAndRestauranteIdAndCargoOrigemId(
				alvo.getId(), restauranteId, cargoId)) {
			doRestaurante.delete(linha);
			auditar(chamador, alvo, restauranteId, linha.getAuthority(), AcaoPermissao.REVOGAR);
		}
	}

	public boolean podeAtribuir(Usuario chamador, Iterable<AuthorityFuncao> authorities) {
		if (authorities == null) {
			return false;
		}
		for (AuthorityFuncao authority : authorities) {
			if (CatalogoFuncaoPadrao.dePlataforma(authority)) {
				return false;
			}
			if (chamador.getPerfil() == Perfil.GESTOR && authority == AuthorityFuncao.EQUIPE_GESTOR_GERIR) {
				return false;
			}
			if (!autorizacao.podeConceder(chamador, authority)) {
				return false;
			}
		}
		return true;
	}

	private void concederAuthority(
			Usuario chamador,
			Usuario alvo,
			Long restauranteId,
			AuthorityFuncao authority,
			Long cargoId,
			AcaoPermissao acao) {
		if (restauranteId == null) {
			if (globais.existsByUsuarioIdAndAuthority(alvo.getId(), authority)) {
				return;
			}
			UsuarioFuncaoGlobal linha = new UsuarioFuncaoGlobal();
			linha.setUsuarioId(alvo.getId());
			linha.setAuthority(authority);
			linha.setAtribuidoPor(chamador.getId());
			linha.setAtribuidoEm(Instant.now());
			linha.setCargoOrigemId(cargoId);
			globais.save(linha);
			auditar(chamador, alvo, null, authority, acao);
			return;
		}
		if (doRestaurante.existsByUsuarioIdAndRestauranteIdAndAuthority(alvo.getId(), restauranteId, authority)) {
			return;
		}
		UsuarioRestauranteFuncao linha = new UsuarioRestauranteFuncao();
		linha.setUsuarioId(alvo.getId());
		linha.setRestauranteId(restauranteId);
		linha.setAuthority(authority);
		linha.setAtribuidoPor(chamador.getId());
		linha.setAtribuidoEm(Instant.now());
		linha.setCargoOrigemId(cargoId);
		doRestaurante.save(linha);
		auditar(chamador, alvo, restauranteId, authority, acao);
	}

	private void propagar(Long cargoId, Set<AuthorityFuncao> adicionadas, Set<AuthorityFuncao> removidas) {
		Set<String> pares = new LinkedHashSet<>();
		for (UsuarioRestauranteFuncao linha : doRestaurante.findByCargoOrigemId(cargoId)) {
			pares.add(linha.getUsuarioId() + ":" + linha.getRestauranteId());
		}
		for (String par : pares) {
			String[] partes = par.split(":");
			Long usuarioId = Long.valueOf(partes[0]);
			Long restauranteId = Long.valueOf(partes[1]);
			Usuario alvo = carregar(usuarioId);
			Usuario chamador = carregar(escopo.usuario().getId());
			for (AuthorityFuncao authority : adicionadas) {
				concederAuthority(chamador, alvo, restauranteId, authority, cargoId, AcaoPermissao.PROPAGACAO_CARGO);
			}
		}
		for (UsuarioRestauranteFuncao linha : new ArrayList<>(doRestaurante.findByCargoOrigemId(cargoId))) {
			if (!removidas.contains(linha.getAuthority())) {
				continue;
			}
			doRestaurante.delete(linha);
			auditar(carregar(escopo.usuario().getId()), carregar(linha.getUsuarioId()), linha.getRestauranteId(),
					linha.getAuthority(), AcaoPermissao.PROPAGACAO_CARGO);
		}
	}

	private Set<AuthorityFuncao> validar(Usuario admin, List<AuthorityFuncao> lista) {
		if (lista == null || lista.isEmpty()) {
			throw new RegraDeNegocioException("Informe ao menos uma função");
		}
		Set<AuthorityFuncao> saida = EnumSet.noneOf(AuthorityFuncao.class);
		for (AuthorityFuncao authority : lista) {
			if (authority == null) {
				continue;
			}
			if (CatalogoFuncaoPadrao.dePlataforma(authority) || !autorizacao.podeConceder(admin, authority)) {
				throw new AccessDeniedException("Acesso negado");
			}
			saida.add(authority);
		}
		if (saida.isEmpty()) {
			throw new RegraDeNegocioException("Informe ao menos uma função");
		}
		return saida;
	}

	private void exigirCargoDaRede(Usuario chamador, CargoRede cargo) {
		Long adminId = adminDaRede(chamador);
		if (adminId == null || !adminId.equals(cargo.getAdminId()) || !cargo.isAtivo()) {
			throw new AccessDeniedException("Acesso negado");
		}
	}

	private Long adminDaRede(Usuario chamador) {
		// vinculo, nao funcao
		if (chamador.getPerfil() == Perfil.ADMINISTRADOR) {
			return chamador.getId();
		}
		if (chamador.getPerfil() == Perfil.GESTOR
				&& chamador.getRestaurante() != null
				&& chamador.getRestaurante().getAdmin() != null) {
			return chamador.getRestaurante().getAdmin().getId();
		}
		return null;
	}

	private Usuario exigirCatalogo() {
		// vinculo, nao funcao
		EscopoOperacao.administrador(escopo.usuario());
		autorizacao.exigir(AuthorityFuncao.CATALOGO_FUNCOES_GERIR);
		return carregar(escopo.usuario().getId());
	}

	private Set<AuthorityFuncao> authorities(Long cargoId) {
		Set<AuthorityFuncao> saida = EnumSet.noneOf(AuthorityFuncao.class);
		for (CargoRedeAuthority item : itens.findByCargoRedeId(cargoId)) {
			saida.add(item.getAuthority());
		}
		return saida;
	}

	private void gravarItem(Long cargoId, AuthorityFuncao authority) {
		CargoRedeAuthority item = new CargoRedeAuthority();
		item.setCargoRedeId(cargoId);
		item.setAuthority(authority);
		itens.save(item);
	}

	private List<FuncaoDTO.CargoItem> montar(List<CargoRede> lista) {
		List<FuncaoDTO.CargoItem> saida = new ArrayList<>();
		for (CargoRede cargo : lista) {
			saida.add(item(cargo, authorities(cargo.getId())));
		}
		return saida;
	}

	private FuncaoDTO.CargoItem item(CargoRede cargo, Set<AuthorityFuncao> authorities) {
		return new FuncaoDTO.CargoItem(
				cargo.getId(),
				cargo.getNome(),
				cargo.getDescricao(),
				cargo.isAtivo(),
				List.copyOf(authorities));
	}

	private CargoRede carregarCargo(Long id) {
		return cargos.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cargo não encontrado"));
	}

	private Usuario carregar(Long id) {
		return usuarios.findParaEscopo(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
	}

	private void auditar(Usuario chamador, Usuario alvo, Long restauranteId, AuthorityFuncao authority, AcaoPermissao acao) {
		AuditoriaPermissao registro = new AuditoriaPermissao();
		registro.setDataHora(Instant.now());
		registro.setAutorId(chamador == null ? null : chamador.getId());
		registro.setAlvoId(alvo.getId());
		registro.setRestauranteId(restauranteId);
		registro.setAuthority(authority.name());
		registro.setAcao(acao);
		auditoria.save(registro);
	}
}
