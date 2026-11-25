package com.projeto.seguranca;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.projeto.entity.Usuario;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.Perfil;

public class UsuarioDetails implements UserDetails {

	private final Long id;
	private final String email;
	private final String senhaHash;
	private final Perfil perfil;
	private final boolean ativo;
	private final List<GrantedAuthority> autoridades;

	public UsuarioDetails(Usuario usuario) {
		this(usuario, List.of());
	}

	/**
	 * O prefixo da função é {@code FUNCAO_}. O cargo continua {@code ROLE_<perfil>}.
	 */
	public UsuarioDetails(Usuario usuario, Collection<AuthorityFuncao> funcoes) {
		this.id = usuario.getId();
		this.email = usuario.getEmail();
		this.senhaHash = usuario.getSenhaHash();
		this.perfil = usuario.getPerfil();
		this.ativo = usuario.isAtivo();
		List<GrantedAuthority> lista = new ArrayList<>();
		lista.add(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
		if (funcoes != null) {
			for (AuthorityFuncao funcao : funcoes) {
				lista.add(new SimpleGrantedAuthority("FUNCAO_" + funcao.name()));
			}
		}
		this.autoridades = List.copyOf(lista);
	}

	public UsuarioAtual atual() {
		return new UsuarioAtual(id, perfil, email);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return autoridades;
	}

	@Override
	public String getPassword() {
		return senhaHash;
	}

	@Override
	public String getUsername() {
		return email;
	}

	@Override
	public boolean isEnabled() {
		return ativo;
	}
}
