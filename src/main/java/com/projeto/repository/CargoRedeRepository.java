package com.projeto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.entity.CargoRede;

public interface CargoRedeRepository extends JpaRepository<CargoRede, Long> {

	List<CargoRede> findByAdminIdAndAtivoTrueOrderByNomeAsc(Long adminId);

	List<CargoRede> findByAdminIdOrderByNomeAsc(Long adminId);

	Optional<CargoRede> findByAdminIdAndNome(Long adminId, String nome);
}
