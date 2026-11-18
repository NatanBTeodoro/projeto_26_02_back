package com.projeto.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.dto.TiDTO;
import com.projeto.service.TiService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ti/usuarios")
public class TiUsuarioController {

	private final TiService tiService;

	public TiUsuarioController(TiService tiService) {
		this.tiService = tiService;
	}

	@GetMapping
	public List<TiDTO.UsuarioTi> listar() {
		return tiService.listarUsuarios();
	}

	@PostMapping
	public TiDTO.UsuarioTi criar(@Valid @RequestBody TiDTO.Create dto) {
		return tiService.criarUsuario(dto);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		tiService.removerUsuario(id);
		return ResponseEntity.noContent().build();
	}
}
