package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.projeto.entity.CargoRede;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.UsuarioRestauranteFuncao;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.Perfil;
import com.projeto.repository.AuditoriaPermissaoRepository;
import com.projeto.repository.CargoRedeAuthorityRepository;
import com.projeto.repository.CargoRedeRepository;
import com.projeto.repository.UsuarioFuncaoGlobalRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.repository.UsuarioRestauranteFuncaoRepository;
import com.projeto.seguranca.EscopoOperacao;

@ExtendWith(MockitoExtension.class)
class CargoRedeServiceTest {

	@Mock
	private EscopoOperacao escopo;
	@Mock
	private Autorizacao autorizacao;
	@Mock
	private UsuarioRepository usuarios;
	@Mock
	private CargoRedeRepository cargos;
	@Mock
	private CargoRedeAuthorityRepository itens;
	@Mock
	private UsuarioRestauranteFuncaoRepository doRestaurante;
	@Mock
	private UsuarioFuncaoGlobalRepository globais;
	@Mock
	private AuditoriaPermissaoRepository auditoria;

	private CargoRedeService service;
	private Usuario admin;
	private Usuario salao;

	@BeforeEach
	void preparar() {
		service = new CargoRedeService(escopo, autorizacao, usuarios, cargos, itens, doRestaurante, globais, auditoria);
		admin = new Usuario();
		admin.setId(1L);
		admin.setPerfil(Perfil.ADMINISTRADOR);
		salao = new Usuario();
		salao.setId(3L);
		salao.setPerfil(Perfil.USUARIO);
		Restaurante casa = new Restaurante();
		casa.setId(9L);
		casa.setAdmin(admin);
		salao.setRestaurante(casa);
	}

	@Test
	void caixaNaoEstornaPedidoPagoESupervisorEstorna() {
		var caixa = CargosPadrao.porNome("Caixa");
		assertFalse(caixa.authorities().contains(AuthorityFuncao.PEDIDO_ESTORNAR_PAGO));
		assertTrue(caixa.authorities().contains(AuthorityFuncao.COMANDA_FECHAR));
		assertTrue(caixa.authorities().contains(AuthorityFuncao.PEDIDO_ESTORNAR));
		assertTrue(CargosPadrao.porNome("Supervisor").authorities().contains(AuthorityFuncao.PEDIDO_ESTORNAR_PAGO));
		assertEquals(6, CargosPadrao.todos().size());
	}

	@Test
	void adminNaoColocaPlataformaNoCargo() {
		when(escopo.usuario()).thenReturn(admin);
		when(usuarios.findParaEscopo(1L)).thenReturn(Optional.of(admin));

		assertThrows(AccessDeniedException.class, () -> service.criar(new FuncaoDTO.CargoCriar(
				"Estoquista",
				null,
				List.of(AuthorityFuncao.PLATAFORMA_TI_GERIR))));
		verify(cargos, never()).save(any());
	}

	@Test
	void atribuirCaixaGravaAsAuthoritiesComOCargo() {
		CargoRede caixa = new CargoRede();
		caixa.setId(7L);
		caixa.setAdminId(1L);
		caixa.setAtivo(true);
		caixa.setNome("Caixa");
		when(cargos.findById(7L)).thenReturn(Optional.of(caixa));
		when(itens.findByCargoRedeId(7L)).thenReturn(CargosPadrao.porNome("Caixa").authorities().stream().map(authority -> {
			var item = new com.projeto.entity.CargoRedeAuthority();
			item.setCargoRedeId(7L);
			item.setAuthority(authority);
			return item;
		}).toList());
		when(autorizacao.podeConceder(any(), any())).thenReturn(true);
		when(doRestaurante.existsByUsuarioIdAndRestauranteIdAndAuthority(any(), any(), any())).thenReturn(false);
		when(doRestaurante.save(any(UsuarioRestauranteFuncao.class))).thenAnswer(chamada -> chamada.getArgument(0));

		service.conceder(admin, salao, 9L, 7L);

		verify(doRestaurante, org.mockito.Mockito.times(8)).save(org.mockito.ArgumentMatchers.argThat(linha ->
				Long.valueOf(7L).equals(linha.getCargoOrigemId())));
		verify(auditoria, org.mockito.Mockito.times(8)).save(any());
	}
}
