package com.projeto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

	List<Categoria> findAllByOrderByNomeAsc();

	List<Categoria> findByRestauranteIdOrderByNomeAsc(Long restauranteId);

	List<Categoria> findByRestauranteId(Long restauranteId);
}
