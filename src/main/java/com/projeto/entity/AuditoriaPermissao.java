package com.projeto.entity;

import java.time.Instant;

import com.projeto.entity.enums.AcaoPermissao;
import com.projeto.entity.enums.AuthorityFuncao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "auditoria_permissao")
public class AuditoriaPermissao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "data_hora", nullable = false)
	private Instant dataHora;

	@Column(name = "autor_id")
	private Long autorId;

	@Column(name = "alvo_id", nullable = false)
	private Long alvoId;

	@Column(name = "restaurante_id")
	private Long restauranteId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private AuthorityFuncao authority;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private AcaoPermissao acao;
}
