package com.projeto.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
		name = "cargo_rede",
		uniqueConstraints = @UniqueConstraint(name = "uk_cargo_rede", columnNames = { "admin_id", "nome" }))
public class CargoRede {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "admin_id", nullable = false)
	private Long adminId;

	@Column(nullable = false, length = 80)
	private String nome;

	@Column(length = 400)
	private String descricao;

	@Column(nullable = false)
	private boolean ativo = true;
}
