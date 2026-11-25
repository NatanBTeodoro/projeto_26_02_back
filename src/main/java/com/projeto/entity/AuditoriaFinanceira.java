package com.projeto.entity;

import java.time.Instant;

import com.projeto.entity.enums.OperacaoAuditoria;

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
@Table(name = "auditoria_financeira")
public class AuditoriaFinanceira {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "data_hora", nullable = false)
	private Instant dataHora;

	@Column(name = "restaurante_id")
	private Long restauranteId;

	@Column(name = "usuario_id")
	private Long usuarioId;

	@Column(name = "perfil_no_momento", length = 20)
	private String perfilNoMomento;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private OperacaoAuditoria operacao;

	@Column(name = "recurso_id")
	private Long recursoId;

	@Column(name = "valor_antes", length = 4000)
	private String valorAntes;

	@Column(name = "valor_depois", length = 4000)
	private String valorDepois;

	@Column(length = 4000)
	private String detalhe;
}
