package com.projeto.seguranca;

import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.projeto.entity.Usuario;
import com.projeto.repository.UsuarioRepository;
import com.projeto.service.FuncaoConsulta;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class FiltroToken extends OncePerRequestFilter {

	private final UsuarioRepository usuarios;
	private final FuncaoConsulta funcoes;

	public FiltroToken(UsuarioRepository usuarios, FuncaoConsulta funcoes) {
		this.usuarios = usuarios;
		this.funcoes = funcoes;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
			String token = cabecalho.substring("Bearer ".length()).trim();
			if (!token.isEmpty()) {
				Optional<Usuario> encontrado = usuarios.findByTokenAcesso(token);
				if (encontrado != null && encontrado.isPresent() && encontrado.get().isAtivo()) {
					Usuario conta = encontrado.get();
					UsuarioDetails detalhes = funcoes == null
							? new UsuarioDetails(conta)
							: new UsuarioDetails(conta, funcoes.doUsuario(conta.getId()));
					var autenticacao = new UsernamePasswordAuthenticationToken(
							detalhes, null, detalhes.getAuthorities());
					SecurityContextHolder.getContext().setAuthentication(autenticacao);
				}
			}
		}
		chain.doFilter(request, response);
	}
}
