package com.projeto.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.projeto.dto.ReclamacaoDTO;
import com.projeto.entity.enums.StatusReclamacao;
import com.projeto.service.ReclamacaoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reclamacoes")
public class ReclamacaoController {

	private final ReclamacaoService reclamacaoService;

	public ReclamacaoController(ReclamacaoService reclamacaoService) {
		this.reclamacaoService = reclamacaoService;
	}

	@GetMapping
	public List<ReclamacaoDTO.Item> listar(
			@RequestParam(required = false) StatusReclamacao status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
			@RequestParam(required = false) String caixa) {
		return reclamacaoService.listar(status, de, ate, caixa);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ReclamacaoDTO.Item criar(
			@RequestParam String assunto,
			@RequestParam String descricao,
			@RequestParam(name = "arquivo", required = false) List<MultipartFile> arquivos) {
		return reclamacaoService.criar(assunto, descricao, arquivos);
	}

	@PutMapping("/{id}")
	public ReclamacaoDTO.Item atualizar(@PathVariable Long id, @Valid @RequestBody ReclamacaoDTO.Atualizacao dto) {
		return reclamacaoService.atualizar(id, dto);
	}

	@GetMapping("/{id}/anexo")
	public ResponseEntity<byte[]> anexo(@PathVariable Long id) {
		ReclamacaoDTO.Anexo anexo = reclamacaoService.baixar(id);
		MediaType tipo = MediaType.APPLICATION_OCTET_STREAM;
		if (anexo.tipo() != null && !anexo.tipo().isBlank()) {
			try {
				tipo = MediaType.parseMediaType(anexo.tipo());
			} catch (InvalidMediaTypeException ex) {
				tipo = MediaType.APPLICATION_OCTET_STREAM;
			}
		}
		ContentDisposition disposicao = ContentDisposition.attachment()
				.filename(anexo.nome(), StandardCharsets.UTF_8)
				.build();
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
				.header("X-Content-Type-Options", "nosniff")
				.header("Content-Security-Policy", "default-src 'none'")
				.contentType(tipo)
				.body(anexo.conteudo());
	}
}
