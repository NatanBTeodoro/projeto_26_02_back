package com.projeto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.entity.UsuarioRestauranteFuncao;
import com.projeto.entity.enums.AuthorityFuncao;

public interface UsuarioRestauranteFuncaoRepository extends JpaRepository<UsuarioRestauranteFuncao, Long> {

	List<UsuarioRestauranteFuncao> findByUsuarioIdAndRestauranteId(Long usuarioId, Long restauranteId);

	boolean existsByUsuarioIdAndRestauranteIdAndAuthority(Long usuarioId, Long restauranteId, AuthorityFuncao authority);

	void deleteByUsuarioIdAndRestauranteIdAndAuthority(Long usuarioId, Long restauranteId, AuthorityFuncao authority);

	List<UsuarioRestauranteFuncao> findByCargoOrigemId(Long cargoOrigemId);

	List<UsuarioRestauranteFuncao> findByUsuarioIdAndRestauranteIdAndCargoOrigemId(
			Long usuarioId, Long restauranteId, Long cargoOrigemId);
}
