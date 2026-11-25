package com.projeto.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.entity.enums.Perfil;
import com.projeto.repository.UsuarioRepository;
import com.projeto.service.CargoRedeService;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CargoBackfill implements ApplicationRunner {

	private final UsuarioRepository usuarios;
	private final CargoRedeService cargos;

	public CargoBackfill(UsuarioRepository usuarios, CargoRedeService cargos) {
		this.usuarios = usuarios;
		this.cargos = cargos;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		usuarios.findByPerfilOrderByNomeAsc(Perfil.ADMINISTRADOR).forEach(admin -> cargos.garantirPadrao(admin.getId()));
		usuarios.findAll().forEach(cargos::vincularPacote);
	}
}
