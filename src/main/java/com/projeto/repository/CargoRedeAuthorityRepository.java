package com.projeto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.entity.CargoRedeAuthority;

public interface CargoRedeAuthorityRepository extends JpaRepository<CargoRedeAuthority, Long> {

	List<CargoRedeAuthority> findByCargoRedeId(Long cargoRedeId);

	void deleteByCargoRedeIdAndAuthority(Long cargoRedeId, com.projeto.entity.enums.AuthorityFuncao authority);
}
