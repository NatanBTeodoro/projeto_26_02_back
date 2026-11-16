package com.projeto.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.dto.CurvaAbcDTO;
import com.projeto.service.CurvaAbcService;

@RestController
@RequestMapping("/api/curva-abc")
public class CurvaAbcController {

	private final CurvaAbcService curvaAbcService;

	public CurvaAbcController(CurvaAbcService curvaAbcService) {
		this.curvaAbcService = curvaAbcService;
	}

	@GetMapping("/modelos")
	public List<CurvaAbcDTO.Modelo> modelos() {
		return curvaAbcService.modelos();
	}

	@GetMapping("/restaurantes")
	public CurvaAbcDTO.Response porRestaurante(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
		return curvaAbcService.porRestaurante(de, ate);
	}

	@GetMapping("/produtos")
	public CurvaAbcDTO.Response porProduto(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
			@RequestParam Long restauranteId) {
		return curvaAbcService.porProduto(de, ate, restauranteId);
	}
}
