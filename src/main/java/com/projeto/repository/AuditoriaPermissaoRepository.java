package com.projeto.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.entity.AuditoriaPermissao;

public interface AuditoriaPermissaoRepository extends JpaRepository<AuditoriaPermissao, Long> {

	List<AuditoriaPermissao> findByAlvoIdAndDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraDesc(
			Long alvoId, Instant inicio, Instant fim);

	List<AuditoriaPermissao> findByRestauranteIdAndDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraDesc(
			Long restauranteId, Instant inicio, Instant fim);

	List<AuditoriaPermissao> findByDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHoraDesc(
			Instant inicio, Instant fim);
}
