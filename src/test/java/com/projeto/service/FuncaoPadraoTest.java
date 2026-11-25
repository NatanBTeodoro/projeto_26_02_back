package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.UsuarioRestauranteFuncao;
import com.projeto.entity.enums.Perfil;
import com.projeto.repository.UsuarioFuncaoGlobalRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.repository.UsuarioRestauranteFuncaoRepository;

@ExtendWith(MockitoExtension.class)
class FuncaoPadraoTest {

	@Mock
	private UsuarioFuncaoGlobalRepository globais;
	@Mock
	private UsuarioRestauranteFuncaoRepository doRestaurante;
	@Mock
	private UsuarioRepository usuarios;

	@Test
	void contaNovaDeUsuarioRecebeOPacoteDoPerfil() {
		Restaurante casa = new Restaurante();
		casa.setId(9L);
		Usuario usuario = new Usuario();
		usuario.setId(30L);
		usuario.setPerfil(Perfil.USUARIO);
		usuario.setRestaurante(casa);
		when(usuarios.findParaEscopo(30L)).thenReturn(Optional.of(usuario));
		when(doRestaurante.existsByUsuarioIdAndRestauranteIdAndAuthority(any(), any(), any())).thenReturn(false);

		new FuncaoPadrao(globais, doRestaurante, usuarios).aplicar(usuario);

		verify(doRestaurante, times(CatalogoFuncaoPadrao.doPerfil(Perfil.USUARIO).size()))
				.save(any(UsuarioRestauranteFuncao.class));
		verify(globais, never()).save(any());
		assertEquals(6, CatalogoFuncaoPadrao.doPerfil(Perfil.USUARIO).size());
	}
}
