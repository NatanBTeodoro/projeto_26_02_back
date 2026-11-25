package com.projeto.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projeto.dto.FuncaoDTO;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.service.CargoRedeService;
import com.projeto.service.FuncaoAtribuicaoService;

@RestController
public class FuncaoController {

	private final FuncaoAtribuicaoService funcoes;
	private final CargoRedeService cargos;

	public FuncaoController(FuncaoAtribuicaoService funcoes, CargoRedeService cargos) {
		this.funcoes = funcoes;
		this.cargos = cargos;
	}

	@GetMapping("/api/funcoes/minhas")
	public List<AuthorityFuncao> minhas() {
		return funcoes.minhas();
	}

	@GetMapping("/api/funcoes/meus-cargos")
	public List<String> meusCargos() {
		return funcoes.meusCargos();
	}

	@GetMapping("/api/usuarios/{id}/funcoes")
	public List<FuncaoDTO.Linha> doUsuario(@PathVariable Long id) {
		return funcoes.listarDoAlvo(id);
	}

	@PostMapping("/api/usuarios/{id}/funcoes")
	public void atribuir(@PathVariable Long id, @RequestBody FuncaoDTO.Atribuicao pedido) {
		funcoes.aplicar(id, pedido);
	}

	@GetMapping("/api/admin/funcoes")
	public List<FuncaoDTO.CatalogoItem> catalogo() {
		return funcoes.listarCatalogo();
	}

	@PostMapping("/api/admin/funcoes")
	public List<FuncaoDTO.CatalogoItem> criar(@RequestBody FuncaoDTO.CatalogoCriar pedido) {
		return funcoes.criarCatalogo(pedido);
	}

	@PutMapping("/api/admin/funcoes/{id}")
	public FuncaoDTO.CatalogoItem atualizar(@PathVariable Long id, @RequestBody FuncaoDTO.CatalogoAtualizar pedido) {
		return funcoes.atualizarCatalogo(id, pedido);
	}

	@GetMapping("/api/auditoria/permissoes")
	public List<FuncaoDTO.AuditoriaItem> auditoria(
			@RequestParam(required = false) Long alvoId,
			@RequestParam(required = false) Long restauranteId,
			@RequestParam(required = false) LocalDate de,
			@RequestParam(required = false) LocalDate ate) {
		return funcoes.consultarAuditoria(alvoId, restauranteId, de, ate);
	}

	@GetMapping("/api/admin/cargos")
	public List<FuncaoDTO.CargoItem> cargos() {
		return cargos.listarDaRede();
	}

	@PostMapping("/api/admin/cargos")
	public FuncaoDTO.CargoItem criarCargo(@RequestBody FuncaoDTO.CargoCriar pedido) {
		return cargos.criar(pedido);
	}

	@PutMapping("/api/admin/cargos/{id}")
	public FuncaoDTO.CargoItem atualizarCargo(@PathVariable Long id, @RequestBody FuncaoDTO.CargoAtualizar pedido) {
		return cargos.atualizar(id, pedido);
	}

	@GetMapping("/api/cargos/atribuiveis")
	public List<FuncaoDTO.CargoItem> atribuiveis() {
		return cargos.atribuiveis();
	}
}
