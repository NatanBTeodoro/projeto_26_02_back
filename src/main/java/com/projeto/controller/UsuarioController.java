package com.projeto.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.dto.SenhaDTO;
import com.projeto.dto.UsuarioDTO;
import com.projeto.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping
	public List<UsuarioDTO.Response> listar(@RequestParam(required = false) Long restauranteId) {
		return usuarioService.listar(restauranteId);
	}

	@PostMapping
	public UsuarioDTO.Response criar(@Valid @RequestBody UsuarioDTO.Create dto) {
		return usuarioService.criar(dto);
	}

	@PutMapping("/{id}")
	public UsuarioDTO.Response atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioDTO.Update dto) {
		return usuarioService.atualizar(id, dto);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id) {
		usuarioService.deletar(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/senha")
	public ResponseEntity<Void> redefinirSenha(@PathVariable Long id, @Valid @RequestBody SenhaDTO.Redefinicao dto) {
		usuarioService.redefinirSenha(id, dto);
		return ResponseEntity.noContent().build();
	}
}
