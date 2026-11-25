package com.projeto.entity;

import java.time.Instant;

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
		name = "usuario_restaurante_funcao",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_usuario_restaurante_funcao",
				columnNames = { "usuario_id", "restaurante_id", "authority" }))
public class UsuarioRestauranteFuncao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "usuario_id", nullable = false)
	private Long usuarioId;

	@Column(name = "restaurante_id", nullable = false)
	private Long restauranteId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AuthorityFuncao authority;

	@Column(name = "atribuido_por")
	private Long atribuidoPor;

	@Column(name = "atribuido_em", nullable = false)
	private Instant atribuidoEm;
}
