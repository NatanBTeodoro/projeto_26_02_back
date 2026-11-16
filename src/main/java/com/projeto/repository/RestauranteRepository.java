package com.projeto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.entity.Restaurante;
import com.projeto.entity.enums.StatusRestaurante;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {

	List<Restaurante> findByAdminIdOrderByNomeAsc(Long adminId);

	List<Restaurante> findByAdminIdAndStatusOrderByNomeAsc(Long adminId, StatusRestaurante status);
}
