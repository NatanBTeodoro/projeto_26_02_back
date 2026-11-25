package com.projeto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.entity.FuncaoRede;
import com.projeto.entity.enums.AuthorityFuncao;

public interface FuncaoRedeRepository extends JpaRepository<FuncaoRede, Long> {

	List<FuncaoRede> findByAdminIdAndAtivoTrueOrderByRotuloAsc(Long adminId);

	boolean existsByAdminIdAndAuthority(Long adminId, AuthorityFuncao authority);
}
