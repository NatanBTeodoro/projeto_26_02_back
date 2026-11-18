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
import com.projeto.service.NomeDuplicado;

@Component
@ConditionalOnProperty(name = "app.semente.habilitada", havingValue = "true")
public class SementeDesenvolvimento implements ApplicationRunner {

	private final UsuarioRepository usuarioRepository;
	private final PasswordEncoder passwordEncoder;
	private final String email;
	private final String senha;
	private final String emailTi;
	private final String senhaTi;

	public SementeDesenvolvimento(
			UsuarioRepository usuarioRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.semente.email}") String email,
			@Value("${app.semente.senha}") String senha,
			@Value("${app.semente.ti.email}") String emailTi,
			@Value("${app.semente.ti.senha}") String senhaTi) {
		this.usuarioRepository = usuarioRepository;
		this.passwordEncoder = passwordEncoder;
		this.email = email;
		this.senha = senha;
		this.emailTi = emailTi;
		this.senhaTi = senhaTi;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (usuarioRepository.count() == 0) {
			criar(email, senha);
			return;
		}
		if (usuarioRepository.existsByPerfil(Perfil.TI)) {
			return;
		}
		String reserva = emailTi == null ? "" : emailTi.trim().toLowerCase(Locale.ROOT);
		String principal = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
		if (reserva.isBlank() || reserva.equals(principal) || usuarioRepository.existsByEmailIgnoreCase(reserva)) {
			return;
		}
		criar(reserva, senhaTi);
	}

	private void criar(String emailConta, String senhaConta) {
		Usuario ti = new Usuario();
		ti.setNome(NomeDuplicado.reservar("Equipe de TI", usuarioRepository.nomesSemRestaurante()));
		ti.setEmail(emailConta.trim().toLowerCase(Locale.ROOT));
		ti.setSenhaHash(passwordEncoder.encode(senhaConta));
		ti.setPerfil(Perfil.TI);
		ti.setAtivo(true);
		usuarioRepository.save(ti);
	}
}
