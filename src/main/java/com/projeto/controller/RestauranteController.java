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
import org.springframework.web.bind.annotation.RestController;

import com.projeto.dto.RestauranteDTO;
import com.projeto.service.RestauranteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/restaurantes")
public class RestauranteController {

	private final RestauranteService restauranteService;

	public RestauranteController(RestauranteService restauranteService) {
		this.restauranteService = restauranteService;
	}

	@GetMapping
	public List<RestauranteDTO.Response> listar() {
		return restauranteService.listar();
	}

	@PostMapping
	public RestauranteDTO.Response criar(@Valid @RequestBody RestauranteDTO.Create dto) {
		return restauranteService.criar(dto);
	}

	@PutMapping("/{id}")
	public RestauranteDTO.Response atualizar(@PathVariable Long id, @Valid @RequestBody RestauranteDTO.Update dto) {
		return restauranteService.atualizar(id, dto);
	}

	@PostMapping("/{id}/inativar")
	public RestauranteDTO.Response inativar(@PathVariable Long id) {
		return restauranteService.inativar(id);
	}

	@PostMapping("/{id}/reativar")
	public RestauranteDTO.Response reativar(@PathVariable Long id) {
		return restauranteService.reativar(id);
	}

	@PostMapping("/{id}/fechar")
	public RestauranteDTO.Response fechar(@PathVariable Long id) {
		return restauranteService.fechar(id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> apagar(@PathVariable Long id) {
		restauranteService.apagar(id);
		return ResponseEntity.noContent().build();
	}
}
