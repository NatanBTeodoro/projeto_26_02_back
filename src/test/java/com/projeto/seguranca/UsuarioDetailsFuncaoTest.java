package com.projeto.seguranca;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.projeto.entity.Usuario;

import com.projeto.entity.enums.Perfil;
import com.projeto.catalogo.CatalogoFuncaoPadrao;

class UsuarioDetailsFuncaoTest {

	@Test
	void tokenCarregaCargoEFuncoes() {
		assertTrue(autoridades(Perfil.TI).contains("ROLE_TI"));
		assertTrue(autoridades(Perfil.TI).contains("FUNCAO_PLATAFORMA_TI_GERIR"));
		assertTrue(autoridades(Perfil.ADMINISTRADOR).contains("ROLE_ADMINISTRADOR"));
		assertTrue(autoridades(Perfil.ADMINISTRADOR).contains("FUNCAO_AUDITORIA_VER"));
		assertTrue(autoridades(Perfil.ADMINISTRADOR).contains("FUNCAO_COMANDA_LER"));
		assertTrue(autoridades(Perfil.GESTOR).contains("ROLE_GESTOR"));
		assertTrue(autoridades(Perfil.GESTOR).contains("FUNCAO_COMANDA_FECHAR"));
		assertTrue(autoridades(Perfil.USUARIO).contains("ROLE_USUARIO"));
		assertTrue(autoridades(Perfil.USUARIO).contains("FUNCAO_PEDIDO_LANCAR"));
		assertTrue(autoridades(Perfil.USUARIO).stream().noneMatch(nome -> nome.equals("FUNCAO_CARDAPIO_ESCREVER")));
	}

	private List<String> autoridades(Perfil perfil) {
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setEmail(perfil.name().toLowerCase() + "@teste");
		usuario.setSenhaHash("hash");
		usuario.setPerfil(perfil);
		usuario.setAtivo(true);
		return new UsuarioDetails(usuario, CatalogoFuncaoPadrao.doPerfil(perfil)).getAuthorities().stream()
				.map(autoridade -> autoridade.getAuthority())
				.toList();
	}
}
