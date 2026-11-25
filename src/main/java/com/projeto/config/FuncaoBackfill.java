package com.projeto.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.repository.UsuarioRepository;
import com.projeto.service.FuncaoPadrao;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class FuncaoBackfill implements ApplicationRunner {

	private final UsuarioRepository usuarios;
	private final FuncaoPadrao funcaoPadrao;

	public FuncaoBackfill(UsuarioRepository usuarios, FuncaoPadrao funcaoPadrao) {
		this.usuarios = usuarios;
		this.funcaoPadrao = funcaoPadrao;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		usuarios.findAll().forEach(funcaoPadrao::aplicar);
	}
}
