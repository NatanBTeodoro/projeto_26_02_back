package com.projeto.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projeto.entity.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

	List<Pedido> findAllByOrderByDataHoraDesc();

	List<Pedido> findByDataHoraGreaterThanEqualAndDataHoraLessThan(Instant inicio, Instant fim);

	List<Pedido> findByRestauranteIdOrderByDataHoraDesc(Long restauranteId);

	List<Pedido> findByUsuarioIdOrderByDataHoraDesc(Long usuarioId);

	List<Pedido> findByRestauranteId(Long restauranteId);

	List<Pedido> findByRestauranteIdAndDataHoraGreaterThanEqualAndDataHoraLessThan(
			Long restauranteId, Instant inicio, Instant fim);

	List<Pedido> findByRestauranteIdInAndDataHoraGreaterThanEqualAndDataHoraLessThan(
			Collection<Long> restauranteIds, Instant inicio, Instant fim);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Pedido p set p.usuario = null where p.usuario.id = :usuarioId")
	int desvincularUsuario(@Param("usuarioId") Long usuarioId);
}
