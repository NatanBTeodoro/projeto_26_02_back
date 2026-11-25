package com.projeto.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.projeto.dto.UsuarioDTO;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.Perfil;
import com.projeto.entity.enums.StatusRestaurante;
import com.projeto.repository.ComandaRepository;
import com.projeto.repository.PedidoRepository;
import com.projeto.repository.RestauranteRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.seguranca.EscopoOperacao;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceCriarFuncaoTest {

	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private RestauranteRepository restauranteRepository;
	@Mock
	private PedidoRepository pedidoRepository;
	@Mock
	private ComandaRepository comandaRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private EscopoOperacao escopo;
	@Mock
	private FuncaoPadrao funcaoPadrao;
	@Mock
	private CargoRedeService cargoRede;
	@Mock
	private FuncaoAtribuicaoService funcoes;
	@Mock
	private Autorizacao autorizacao;

	@Test
	void criarAplicaOPacotePadraoNaContaNova() {
		Restaurante casa = new Restaurante();
		casa.setId(9L);
		casa.setStatus(StatusRestaurante.ATIVO);
		Usuario gestor = new Usuario();
		gestor.setId(2L);
		gestor.setPerfil(Perfil.GESTOR);
		gestor.setRestaurante(casa);
		when(escopo.usuario()).thenReturn(gestor);
		when(usuarioRepository.existsByEmailIgnoreCase("ana@casa.com")).thenReturn(false);
		when(usuarioRepository.nomesDoRestaurante(9L)).thenReturn(List.of());
		when(passwordEncoder.encode("Senha123!")).thenReturn("hash");
		when(usuarioRepository.save(any(Usuario.class))).thenAnswer(chamada -> {
			Usuario salvo = chamada.getArgument(0);
			salvo.setId(30L);
			return salvo;
		});
		UsuarioService service = new UsuarioService(
				usuarioRepository,
				restauranteRepository,
				pedidoRepository,
				comandaRepository,
				passwordEncoder,
				escopo,
				funcaoPadrao,
				cargoRede,
				funcoes,
				autorizacao,
				"admin@admin.com");

		service.criar(new UsuarioDTO.Create("Ana", "ana@casa.com", "Senha123!", Perfil.USUARIO, null));

		ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
		verify(funcaoPadrao).aplicar(captor.capture());
		verify(cargoRede).vincularPacote(captor.getValue());
		org.junit.jupiter.api.Assertions.assertEquals(30L, captor.getValue().getId());
		org.junit.jupiter.api.Assertions.assertEquals(Perfil.USUARIO, captor.getValue().getPerfil());
		org.junit.jupiter.api.Assertions.assertEquals(9L, captor.getValue().getRestaurante().getId());
	}
}
