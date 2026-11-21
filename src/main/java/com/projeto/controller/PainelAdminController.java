package com.projeto.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.dto.PainelAdminDTO;
import com.projeto.service.PainelAdminService;

@RestController
@RequestMapping("/api/painel-admin")
public class PainelAdminController {

	private final PainelAdminService painelAdminService;

	public PainelAdminController(PainelAdminService painelAdminService) {
		this.painelAdminService = painelAdminService;
	}

	@GetMapping
	public PainelAdminDTO.Response consultar(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
		return painelAdminService.consultar(de, ate);
	}
}
