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

	List<Pedido> findByComandaId(Long comandaId);

	List<Pedido> findByRestauranteIdAndDataHoraGreaterThanEqualAndDataHoraLessThan(
			Long restauranteId, Instant inicio, Instant fim);

	List<Pedido> findByRestauranteIdInAndDataHoraGreaterThanEqualAndDataHoraLessThan(
			Collection<Long> restauranteIds, Instant inicio, Instant fim);

	@Query("""
			select min(p.dataHora) from Pedido p
			where p.restaurante.id in :ids and p.status = com.projeto.entity.enums.StatusPedido.PAGO
			""")
	Instant primeiraVendaAtiva(@Param("ids") Collection<Long> ids);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Pedido p set p.usuario = null where p.usuario.id = :usuarioId")
	int desvincularUsuario(@Param("usuarioId") Long usuarioId);
}
