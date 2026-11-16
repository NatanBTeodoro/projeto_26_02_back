package com.projeto.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.projeto.entity.Pedido;
import com.projeto.entity.PedidoAlteracao;
import com.projeto.entity.PedidoItem;
import com.projeto.entity.enums.StatusPedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PedidoDTO {

	private PedidoDTO() {
	}

	public record ItemCreate(
			@NotNull(message = "Informe o produto") Long produtoId,
			@NotNull(message = "Informe a quantidade") @Min(value = 1, message = "A quantidade precisa ser maior que zero") Integer quantidade) {
	}

	public record Create(
			Instant dataHora,
			Long comandaId,
			Long usuarioId,
			@NotNull(message = "Informe o número da mesa") @Min(value = 1, message = "Informe o número da mesa") Integer mesa,
			@NotBlank(message = "Informe o nome do cliente") @Size(max = 120) String nomeCliente,
			@NotEmpty(message = "O pedido precisa de ao menos um item") @Valid List<ItemCreate> itens) {
	}

	public record Estorno(@NotBlank(message = "Informe o motivo do estorno") @Size(max = 255) String motivo) {
	}

	public record Cancelamento(@NotBlank(message = "Informe o motivo do cancelamento") @Size(max = 255) String motivo) {
	}

	public record Edicao(
			@NotBlank(message = "Informe o motivo da edição") @Size(max = 255) String motivo,
			@NotNull(message = "Informe o número da mesa") @Min(value = 1, message = "Informe o número da mesa") Integer mesa,
			@NotBlank(message = "Informe o nome do cliente") @Size(max = 120) String nomeCliente,
			@NotEmpty(message = "O pedido precisa de ao menos um item") @Valid List<ItemCreate> itens) {
	}

	public record ItemResponse(
			Long id,
			Long produtoId,
			String produtoNome,
			Integer quantidade,
			BigDecimal precoUnitario,
			BigDecimal custoUnitario) {

		public static ItemResponse de(PedidoItem item) {
			return new ItemResponse(
					item.getId(),
					item.getProduto().getId(),
					item.getProduto().getNome(),
					item.getQuantidade(),
					item.getPrecoUnitario(),
					item.getCustoUnitario());
		}
	}

	public record AlteracaoResponse(Long id, Instant dataHora, String descricao, String motivo, String autor) {

		public static AlteracaoResponse de(PedidoAlteracao alteracao) {
			return new AlteracaoResponse(
					alteracao.getId(),
					alteracao.getDataHora(),
					alteracao.getDescricao(),
					alteracao.getMotivo(),
					alteracao.getAutor());
		}
	}

	public record Response(
			Long id,
			Instant dataHora,
			StatusPedido status,
			String motivoEstorno,
			Instant dataHoraEstorno,
			String autorEstorno,
			String motivoCancelamento,
			Instant dataHoraCancelamento,
			String autorCancelamento,
			Long comandaId,
			Integer mesa,
			String nomeCliente,
			Long usuarioId,
			String usuarioNome,
			Long restauranteId,
			List<ItemResponse> itens,
			List<AlteracaoResponse> alteracoes) {

		public static Response de(Pedido pedido) {
			String nome = pedido.getUsuario() != null && pedido.getUsuario().getNome() != null
					? pedido.getUsuario().getNome()
					: pedido.getUsuarioNome();
			Long usuarioId = pedido.getUsuario() == null ? null : pedido.getUsuario().getId();
			Long comandaId = pedido.getComanda() == null ? null : pedido.getComanda().getId();
			Long restauranteId = pedido.getRestaurante() == null ? null : pedido.getRestaurante().getId();
			List<AlteracaoResponse> historico = pedido.getAlteracoes() == null
					? List.of()
					: pedido.getAlteracoes().stream().map(AlteracaoResponse::de).toList();
			return new Response(
					pedido.getId(),
					pedido.getDataHora(),
					pedido.getStatus(),
					pedido.getMotivoEstorno(),
					pedido.getDataHoraEstorno(),
					pedido.getAutorEstorno(),
					pedido.getMotivoCancelamento(),
					pedido.getDataHoraCancelamento(),
					pedido.getAutorCancelamento(),
					comandaId,
					pedido.getMesa(),
					pedido.getNomeCliente(),
					usuarioId,
					nome,
					restauranteId,
					pedido.getItens().stream().map(ItemResponse::de).toList(),
					historico);
		}
	}
}
