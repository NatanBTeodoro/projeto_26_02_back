package com.projeto.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.projeto.entity.Usuario;
import com.projeto.entity.enums.AuthorityFuncao;

@Component
public class Autorizacao {

	public static final String PREFIXO = "FUNCAO_";

	private final FuncaoConsulta consulta;

	public Autorizacao(FuncaoConsulta consulta) {
		this.consulta = consulta;
	}

	public boolean tem(AuthorityFuncao funcao) {
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		if (autenticacao == null) {
			return false;
		}
		String nome = PREFIXO + funcao.name();
		return autenticacao.getAuthorities().stream().anyMatch(autoridade -> nome.equals(autoridade.getAuthority()));
	}

	public void exigir(AuthorityFuncao funcao) {
		if (!tem(funcao)) {
			throw new AccessDeniedException("Acesso negado");
		}
	}

	public boolean podeConceder(Usuario chamador, AuthorityFuncao authority) {
		if (chamador == null || authority == null) {
			return false;
		}
		return consulta.doUsuario(chamador.getId()).contains(authority);
	}

	public void exigirPodeConceder(Usuario chamador, AuthorityFuncao authority) {
		if (!podeConceder(chamador, authority)) {
			throw new AccessDeniedException("Acesso negado");
		}
	}
}
