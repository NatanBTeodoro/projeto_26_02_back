package com.projeto.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.projeto.entity.enums.StatusPedido;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pedido")
public class Pedido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "data_hora", nullable = false)
	private Instant dataHora;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusPedido status = StatusPedido.ATIVO;

	@Column(name = "motivo_estorno", length = 255)
	private String motivoEstorno;

	@Column(name = "data_hora_estorno")
	private Instant dataHoraEstorno;

	@Column(name = "autor_estorno", length = 120)
	private String autorEstorno;

	@Column(name = "motivo_cancelamento", length = 255)
	private String motivoCancelamento;

	@Column(name = "data_hora_cancelamento")
	private Instant dataHoraCancelamento;

	@Column(name = "autor_cancelamento", length = 120)
	private String autorCancelamento;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "restaurante_id")
	private Restaurante restaurante;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id")
	private Usuario usuario;

	@Column(name = "usuario_nome", length = 120)
	private String usuarioNome;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "comanda_id")
	private Comanda comanda;

	private Integer mesa;

	@Column(name = "nome_cliente", length = 120)
	private String nomeCliente;

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PedidoItem> itens = new ArrayList<>();

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PedidoAlteracao> alteracoes = new ArrayList<>();
}
