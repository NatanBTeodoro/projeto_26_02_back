package com.projeto.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.dto.CategoriaDTO;
import com.projeto.entity.Categoria;
import com.projeto.entity.Restaurante;
import com.projeto.repository.CategoriaRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RecursoNaoEncontradoException;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

	private final CategoriaRepository categoriaRepository;
	private final EscopoOperacao escopo;

	public CategoriaService(CategoriaRepository categoriaRepository, EscopoOperacao escopo) {
		this.categoriaRepository = categoriaRepository;
		this.escopo = escopo;
	}

	public List<CategoriaDTO.Response> listar() {
		Restaurante restaurante = escopo.restauranteDoContexto();
		return categoriaRepository.findByRestauranteIdOrderByNomeAsc(restaurante.getId()).stream()
				.map(CategoriaDTO.Response::de)
				.toList();
	}

	@Transactional
	public CategoriaDTO.Response criar(CategoriaDTO.Create dto) {
		Restaurante restaurante = escopo.exigirOperavel();
		Categoria categoria = new Categoria();
		categoria.setNome(dto.nome().trim());
		categoria.setAtiva(true);
		categoria.setRestaurante(restaurante);
		return CategoriaDTO.Response.de(categoriaRepository.save(categoria));
	}

	@Transactional
	public CategoriaDTO.Response atualizar(Long id, CategoriaDTO.Update dto) {
		escopo.exigirOperavel();
		Categoria categoria = carregar(id);
		categoria.setNome(dto.nome().trim());
		if (dto.ativa() != null) {
			categoria.setAtiva(dto.ativa());
		}
		return CategoriaDTO.Response.de(categoriaRepository.save(categoria));
	}

	Categoria carregar(Long id) {
		Restaurante restaurante = escopo.restauranteDoContexto();
		Categoria categoria = categoriaRepository.findById(id)
				.orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
		if (categoria.getRestaurante() == null || !categoria.getRestaurante().getId().equals(restaurante.getId())) {
			throw new RecursoNaoEncontradoException("Categoria não encontrada");
		}
		return categoria;
	}
}
