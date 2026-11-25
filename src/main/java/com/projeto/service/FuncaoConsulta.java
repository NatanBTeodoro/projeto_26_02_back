package com.projeto.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.entity.Usuario;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.repository.UsuarioFuncaoGlobalRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.repository.UsuarioRestauranteFuncaoRepository;

@Service
public class FuncaoConsulta {

	private final UsuarioFuncaoGlobalRepository globais;
	private final UsuarioRestauranteFuncaoRepository doRestaurante;
	private final UsuarioRepository usuarios;

	public FuncaoConsulta(
			UsuarioFuncaoGlobalRepository globais,
			UsuarioRestauranteFuncaoRepository doRestaurante,
			UsuarioRepository usuarios) {
		this.globais = globais;
		this.doRestaurante = doRestaurante;
		this.usuarios = usuarios;
	}

	@Transactional(readOnly = true)
	public List<AuthorityFuncao> doUsuario(Long usuarioId) {
		Set<AuthorityFuncao> funcoes = new LinkedHashSet<>();
		globais.findByUsuarioId(usuarioId).forEach(linha -> funcoes.add(linha.getAuthority()));
		usuarios.findParaEscopo(usuarioId).ifPresent(usuario -> acrescentarDoRestaurante(usuario, funcoes));
		return new ArrayList<>(funcoes);
	}

	private void acrescentarDoRestaurante(Usuario usuario, Set<AuthorityFuncao> funcoes) {
		if (usuario.getRestaurante() == null) {
			return;
		}
		doRestaurante.findByUsuarioIdAndRestauranteId(usuario.getId(), usuario.getRestaurante().getId())
				.forEach(linha -> funcoes.add(linha.getAuthority()));
	}
}
