package com.projeto.seguranca;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.repository.UsuarioRepository;
import com.projeto.service.FuncaoConsulta;

@Service
public class UsuarioDetailsService implements UserDetailsService {

	private final UsuarioRepository usuarioRepository;
	private final FuncaoConsulta funcoes;

	public UsuarioDetailsService(UsuarioRepository usuarioRepository, FuncaoConsulta funcoes) {
		this.usuarioRepository = usuarioRepository;
		this.funcoes = funcoes;
	}

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) {
		String normalizado = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
		return usuarioRepository.findByEmailIgnoreCase(normalizado)
				.map(usuario -> new UsuarioDetails(usuario, funcoes.doUsuario(usuario.getId())))
				.orElseThrow(() -> new UsernameNotFoundException("credenciais"));
	}
}
