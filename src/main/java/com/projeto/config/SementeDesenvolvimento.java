package com.projeto.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.entity.Usuario;
import com.projeto.entity.enums.Perfil;
import com.projeto.repository.UsuarioRepository;

@Component
@ConditionalOnProperty(name = "app.semente.habilitada", havingValue = "true")
public class SementeDesenvolvimento implements ApplicationRunner {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String senha;

	public SementeDesenvolvimento(
			UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.semente.email}") String email,
			@Value("${app.semente.senha}") String senha) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.senha = senha;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (usuarioRepository.count() > 0) {
			return;
		}
		Usuario ti = new Usuario();
		ti.setNome("Equipe de TI");
		ti.setEmail(email.trim().toLowerCase(Locale.ROOT));
		ti.setSenhaHash(passwordEncoder.encode(senha));
		ti.setPerfil(Perfil.TI);
		ti.setAtivo(true);
		usuarioRepository.save(ti);
	}
}
