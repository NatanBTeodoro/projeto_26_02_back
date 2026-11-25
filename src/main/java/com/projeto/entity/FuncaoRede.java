package com.projeto.entity;

import com.projeto.entity.enums.AuthorityFuncao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
		name = "funcao_rede",
		uniqueConstraints = @UniqueConstraint(name = "uk_funcao_rede", columnNames = { "admin_id", "authority" }))
public class FuncaoRede {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "admin_id", nullable = false)
	private Long adminId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AuthorityFuncao authority;

	@Column(nullable = false, length = 80)
	private String rotulo;

	@Column(length = 400)
	private String descricao;

	@Column(nullable = false)
	private boolean ativo = true;
}
