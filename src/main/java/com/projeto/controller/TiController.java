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
@RequestMapping("/api/ti/admins")
public class TiController {

	private final TiService tiService;

	public TiController(TiService tiService) {
		this.tiService = tiService;
	}

	@GetMapping
	public List<TiDTO.Admin> listar() {
		return tiService.listar();
	}

	@PostMapping
	public TiDTO.Admin criar(@Valid @RequestBody TiDTO.Create dto) {
		return tiService.criar(dto);
	}

	@PostMapping("/{id}/inativar")
	public TiDTO.Admin inativar(@PathVariable Long id) {
		return tiService.inativar(id);
	}

	@PostMapping("/{id}/reativar")
	public TiDTO.Admin reativar(@PathVariable Long id) {
		return tiService.reativar(id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deletar(@PathVariable Long id) {
		tiService.deletar(id);
		return ResponseEntity.noContent().build();
	}
}
