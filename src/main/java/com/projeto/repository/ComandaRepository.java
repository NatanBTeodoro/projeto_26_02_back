package com.projeto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projeto.entity.Comanda;

public interface ComandaRepository extends JpaRepository<Comanda, Long> {

	List<Comanda> findByRestauranteId(Long restauranteId);

	List<Comanda> findByUsuarioIdOrderByIdDesc(Long usuarioId);

	List<Comanda> findByRestauranteIdAndUsuarioIdOrderByIdDesc(Long restauranteId, Long usuarioId);

	@Query("select coalesce(max(c.numero), 0) from Comanda c where c.restaurante.id = :restauranteId")
	int maiorNumero(@Param("restauranteId") Long restauranteId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Comanda c set c.usuario = null where c.usuario.id = :usuarioId")
	int desvincularUsuario(@Param("usuarioId") Long usuarioId);
}
