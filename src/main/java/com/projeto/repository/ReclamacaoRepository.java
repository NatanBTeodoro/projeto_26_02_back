package com.projeto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.projeto.entity.Reclamacao;

public interface ReclamacaoRepository extends JpaRepository<Reclamacao, Long>, JpaSpecificationExecutor<Reclamacao> {
}
