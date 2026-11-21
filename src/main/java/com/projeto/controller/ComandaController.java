package com.projeto.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.dto.ComandaDTO;
import com.projeto.service.ComandaService;

@RestController
@RequestMapping("/api/comandas")
public class ComandaController {

	private final ComandaService comandaService;

	public ComandaController(ComandaService comandaService) {
		this.comandaService = comandaService;
	}

	@GetMapping
	public List<ComandaDTO.Response> listar(@RequestParam(required = false) Long usuarioId) {
		return comandaService.listar(usuarioId);
	}

	@PostMapping
	public ComandaDTO.Response abrir(@RequestBody(required = false) ComandaDTO.Create dto) {
		return comandaService.abrir(dto);
	}

	@PostMapping("/{id}/fechar")
	public ComandaDTO.Response fechar(@PathVariable Long id) {
		return comandaService.fechar(id);
	}
}
