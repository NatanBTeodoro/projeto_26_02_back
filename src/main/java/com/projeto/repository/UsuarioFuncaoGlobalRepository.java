package com.projeto.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projeto.entity.UsuarioFuncaoGlobal;
import com.projeto.entity.enums.AuthorityFuncao;

public interface UsuarioFuncaoGlobalRepository extends JpaRepository<UsuarioFuncaoGlobal, Long> {

	List<UsuarioFuncaoGlobal> findByUsuarioId(Long usuarioId);

	boolean existsByUsuarioIdAndAuthority(Long usuarioId, AuthorityFuncao authority);

	void deleteByUsuarioIdAndAuthority(Long usuarioId, AuthorityFuncao authority);
}
