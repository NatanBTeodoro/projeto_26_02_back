package com.projeto.entity;

import java.time.Instant;

import com.projeto.entity.enums.DestinoReclamacao;
import com.projeto.entity.enums.StatusReclamacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reclamacao")
public class Reclamacao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "autor_id")
	private Usuario autor;

	@Column(name = "autor_nome", nullable = false, length = 120)
	private String autorNome;

	@Column(name = "autor_email", nullable = false, length = 255)
	private String autorEmail;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm;

	@Column(nullable = false, length = 150)
	private String assunto;

	@Column(nullable = false, length = 4000)
	private String descricao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusReclamacao status;

	@Column(length = 4000)
	private String resposta;

	@Column(name = "respondido_em")
	private Instant respondidoEm;

	@Column(name = "anexo_nome", length = 255)
	private String anexoNome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DestinoReclamacao destino = DestinoReclamacao.TI;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "admin_destino_id")
	private Usuario adminDestino;

	@Column(name = "restaurante_nome", length = 150)
	private String restauranteNome;
}
