package com.projeto.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.entity.Usuario;
import com.projeto.entity.UsuarioFuncaoGlobal;
import com.projeto.entity.UsuarioRestauranteFuncao;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.repository.UsuarioFuncaoGlobalRepository;
import com.projeto.repository.UsuarioRestauranteFuncaoRepository;

@Service
public class FuncaoPadrao {

	private final UsuarioFuncaoGlobalRepository globais;
	private final UsuarioRestauranteFuncaoRepository doRestaurante;
	private final com.projeto.repository.UsuarioRepository usuarios;

	public FuncaoPadrao(
			UsuarioFuncaoGlobalRepository globais,
			UsuarioRestauranteFuncaoRepository doRestaurante,
			com.projeto.repository.UsuarioRepository usuarios) {
		this.globais = globais;
		this.doRestaurante = doRestaurante;
		this.usuarios = usuarios;
	}

	@Transactional
	public void aplicar(Usuario usuario) {
		Usuario completo = usuarios.findParaEscopo(usuario.getId()).orElse(usuario);
		Long restauranteId = completo.getRestaurante() == null ? null : completo.getRestaurante().getId();
		usuario = completo;
		for (AuthorityFuncao authority : CatalogoFuncaoPadrao.doPerfil(usuario.getPerfil())) {
			if (restauranteId == null) {
				if (globais.existsByUsuarioIdAndAuthority(usuario.getId(), authority)) {
					continue;
				}
				UsuarioFuncaoGlobal linha = new UsuarioFuncaoGlobal();
				linha.setUsuarioId(usuario.getId());
				linha.setAuthority(authority);
				linha.setAtribuidoEm(Instant.now());
				globais.save(linha);
			} else {
				if (doRestaurante.existsByUsuarioIdAndRestauranteIdAndAuthority(usuario.getId(), restauranteId, authority)) {
					continue;
				}
				UsuarioRestauranteFuncao linha = new UsuarioRestauranteFuncao();
				linha.setUsuarioId(usuario.getId());
				linha.setRestauranteId(restauranteId);
				linha.setAuthority(authority);
				linha.setAtribuidoEm(Instant.now());
				doRestaurante.save(linha);
			}
		}
	}
}
