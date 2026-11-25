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
		name = "cargo_rede_authority",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_cargo_rede_authority",
				columnNames = { "cargo_rede_id", "authority" }))
public class CargoRedeAuthority {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "cargo_rede_id", nullable = false)
	private Long cargoRedeId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AuthorityFuncao authority;
}
