package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.projeto.dto.FuncaoDTO;
import com.projeto.entity.AuditoriaPermissao;
import com.projeto.entity.FuncaoRede;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.AcaoPermissao;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.Perfil;
import com.projeto.repository.AuditoriaPermissaoRepository;
import com.projeto.repository.FuncaoRedeRepository;
import com.projeto.repository.RestauranteRepository;
import com.projeto.repository.UsuarioFuncaoGlobalRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.repository.UsuarioRestauranteFuncaoRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RegraDeNegocioException;

@ExtendWith(MockitoExtension.class)
class FuncaoAtribuicaoServiceTest {

	@Mock
	private EscopoOperacao escopo;
	@Mock
	private Autorizacao autorizacao;
	@Mock
	private UsuarioRepository usuarios;
	@Mock
	private UsuarioFuncaoGlobalRepository globais;
	@Mock
	private UsuarioRestauranteFuncaoRepository doRestaurante;
	@Mock
	private FuncaoRedeRepository catalogo;
	@Mock
	private AuditoriaPermissaoRepository auditoria;
	@Mock
	private FuncaoConsulta consulta;
	@Mock
	private RestauranteRepository restaurantes;

	private FuncaoAtribuicaoService service;
	private Usuario admin;
	private Usuario gestor;
	private Usuario salao;
	private Restaurante casa;

	@BeforeEach
	void preparar() {
		service = new FuncaoAtribuicaoService(
				escopo, autorizacao, usuarios, globais, doRestaurante, catalogo, auditoria, consulta, org.mockito.Mockito.mock(CargoRedeService.class), restaurantes);
		admin = pessoa(1L, Perfil.ADMINISTRADOR);
		gestor = pessoa(2L, Perfil.GESTOR);
		salao = pessoa(3L, Perfil.USUARIO);
		casa = new Restaurante();
		casa.setId(9L);
		casa.setAdmin(admin);
		gestor.setRestaurante(casa);
		salao.setRestaurante(casa);
	}

	@Test
	void adminConcedeCardapioEAuditaUmaVez() {
		quando(admin, salao);
		when(doRestaurante.existsByUsuarioIdAndRestauranteIdAndAuthority(3L, 9L, AuthorityFuncao.CARDAPIO_ESCREVER))
				.thenReturn(false);

		service.aplicar(3L, new FuncaoDTO.Atribuicao(9L, List.of(AuthorityFuncao.CARDAPIO_ESCREVER), List.of()));

		verify(doRestaurante).save(any());
		verify(auditoria).save(any(AuditoriaPermissao.class));
	}

	@Test
	void adminNaoConcedeFuncaoDePlataforma() {
		quando(admin, salao);

		assertThrows(RegraDeNegocioException.class, () -> service.aplicar(
				3L, new FuncaoDTO.Atribuicao(9L, List.of(AuthorityFuncao.PLATAFORMA_TI_GERIR), List.of())));
		verify(auditoria, never()).save(any());
	}

	@Test
	void ninguemConcedeAuthorityQueNaoTem() {
		quando(admin, salao);

		assertThrows(RegraDeNegocioException.class, () -> service.aplicar(
				3L, new FuncaoDTO.Atribuicao(9L, List.of(AuthorityFuncao.AUDITORIA_VER), List.of())));
		verify(auditoria, never()).save(any());
	}

	@Test
	void gestorConcedeCardapioAoUsuarioDaCasa() {
		quando(gestor, salao);
		when(doRestaurante.existsByUsuarioIdAndRestauranteIdAndAuthority(3L, 9L, AuthorityFuncao.CARDAPIO_ESCREVER))
				.thenReturn(false);

		service.aplicar(3L, new FuncaoDTO.Atribuicao(9L, List.of(AuthorityFuncao.CARDAPIO_ESCREVER), List.of()));

		verify(auditoria).save(org.mockito.ArgumentMatchers.argThat(registro ->
				registro.getAcao() == AcaoPermissao.CONCEDER
						&& "CARDAPIO_ESCREVER".equals(registro.getAuthority())
						&& registro.getAlvoId().equals(3L)));
	}

	@Test
	void gestorNaoConcedeGestaoDeGestor() {
		quando(gestor, salao);

		assertThrows(RegraDeNegocioException.class, () -> service.aplicar(
				3L, new FuncaoDTO.Atribuicao(9L, List.of(AuthorityFuncao.EQUIPE_GESTOR_GERIR), List.of())));
		verify(auditoria, never()).save(any());
	}

	@Test
	void gestorNaoAtribuiEmOutroRestaurante() {
		Restaurante outra = new Restaurante();
		outra.setId(4L);
		outra.setAdmin(admin);
		Usuario deFora = pessoa(8L, Perfil.USUARIO);
		deFora.setRestaurante(outra);
		when(escopo.usuario()).thenReturn(gestor);
		when(usuarios.findParaEscopo(2L)).thenReturn(Optional.of(gestor));
		when(usuarios.findParaEscopo(8L)).thenReturn(Optional.of(deFora));

		assertThrows(AccessDeniedException.class, () -> service.aplicar(
				8L, new FuncaoDTO.Atribuicao(4L, List.of(AuthorityFuncao.CARDAPIO_LER), List.of())));
	}

	@Test
	void adminCriaCatalogoEstoquista() {
		when(escopo.usuario()).thenReturn(admin);
		when(usuarios.findParaEscopo(1L)).thenReturn(Optional.of(admin));
		when(catalogo.existsByAdminIdAndAuthority(1L, AuthorityFuncao.CARDAPIO_LER)).thenReturn(false);
		when(catalogo.existsByAdminIdAndAuthority(1L, AuthorityFuncao.CUSTO_LER)).thenReturn(false);
		when(catalogo.existsByAdminIdAndAuthority(1L, AuthorityFuncao.CARDAPIO_ESCREVER)).thenReturn(false);
		when(catalogo.findByAdminIdAndAtivoTrueOrderByRotuloAsc(1L)).thenReturn(List.of(linha("Estoquista")));

		List<FuncaoDTO.CatalogoItem> criadas = service.criarCatalogo(new FuncaoDTO.CatalogoCriar(
				"Estoquista",
				"Cardápio e custo",
				List.of(AuthorityFuncao.CARDAPIO_LER, AuthorityFuncao.CUSTO_LER, AuthorityFuncao.CARDAPIO_ESCREVER)));

		assertEquals("Estoquista", criadas.get(0).rotulo());
		verify(catalogo, org.mockito.Mockito.times(3)).save(any(FuncaoRede.class));
	}

	@Test
	void consultaDeAuditoriaFiltraPorAlvo() {
		when(escopo.usuario()).thenReturn(admin);
		AuditoriaPermissao doAlvo = registro(3L, 9L);
		when(auditoria.findByAlvoIdAndDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraDesc(
				org.mockito.ArgumentMatchers.eq(3L), any(), any()))
				.thenReturn(List.of(doAlvo));
		when(usuarios.findParaEscopo(3L)).thenReturn(Optional.of(salao));
		admin.setNome("Ana Admin");
		salao.setNome("Bia");
		casa.setNome("Casa Norte");
		when(usuarios.findAllById(any())).thenReturn(List.of(admin, salao));
		when(restaurantes.findAllById(any())).thenReturn(List.of(casa));

		List<FuncaoDTO.AuditoriaItem> itens = service.consultarAuditoria(3L, 9L, null, null);

		assertEquals(1, itens.size());
		assertEquals(3L, itens.get(0).alvoId());
		assertEquals("Ana Admin", itens.get(0).autorNome());
		assertEquals("Bia", itens.get(0).alvoNome());
		assertEquals("Casa Norte", itens.get(0).restauranteNome());
	}

	private void quando(Usuario chamador, Usuario alvo) {
		when(escopo.usuario()).thenReturn(chamador);
		when(usuarios.findParaEscopo(chamador.getId())).thenReturn(Optional.of(chamador));
		when(usuarios.findParaEscopo(alvo.getId())).thenReturn(Optional.of(alvo));
	}

	private Usuario pessoa(Long id, Perfil perfil) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setPerfil(perfil);
		usuario.setNome("Pessoa");
		return usuario;
	}

	private FuncaoRede linha(String rotulo) {
		FuncaoRede linha = new FuncaoRede();
		linha.setId(1L);
		linha.setRotulo(rotulo);
		linha.setAuthority(AuthorityFuncao.CARDAPIO_LER);
		linha.setAtivo(true);
		return linha;
	}

	private AuditoriaPermissao registro(Long alvoId, Long restauranteId) {
		AuditoriaPermissao registro = new AuditoriaPermissao();
		registro.setId(1L);
		registro.setAlvoId(alvoId);
		registro.setRestauranteId(restauranteId);
		registro.setAuthority(AuthorityFuncao.CARDAPIO_ESCREVER.name());
		registro.setAcao(AcaoPermissao.CONCEDER);
		registro.setAutorId(1L);
		registro.setDataHora(java.time.Instant.now());
		return registro;
	}
}
