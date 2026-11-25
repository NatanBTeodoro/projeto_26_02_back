package com.projeto.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.entity.AuditoriaFinanceira;
import com.projeto.entity.Pedido;
import com.projeto.entity.PedidoItem;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.OperacaoAuditoria;
import com.projeto.repository.AuditoriaFinanceiraRepository;

@Service
public class AuditoriaFinanceiraService {

	private static final int LIMITE = 4000;

	private final AuditoriaFinanceiraRepository repositorio;
	private final AuditoriaFinanceiraService self;

	public AuditoriaFinanceiraService(
			AuditoriaFinanceiraRepository repositorio,
			@Lazy AuditoriaFinanceiraService self) {
		this.repositorio = repositorio;
		this.self = self;
	}

	public void registrar(
			Long restauranteId,
			Usuario usuario,
			OperacaoAuditoria operacao,
			Long recursoId,
			String valorAntes,
			String valorDepois,
			String detalhe) {
		try {
			AuditoriaFinanceiraService alvo = self == null ? this : self;
			alvo.gravar(restauranteId, usuario, operacao, recursoId, valorAntes, valorDepois, detalhe);
		} catch (RuntimeException ex) {
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void gravar(
			Long restauranteId,
			Usuario usuario,
			OperacaoAuditoria operacao,
			Long recursoId,
			String valorAntes,
			String valorDepois,
			String detalhe) {
		AuditoriaFinanceira linha = new AuditoriaFinanceira();
		linha.setDataHora(Instant.now());
		linha.setRestauranteId(restauranteId);
		linha.setUsuarioId(usuario == null ? null : usuario.getId());
		linha.setPerfilNoMomento(usuario == null || usuario.getPerfil() == null ? null : usuario.getPerfil().name());
		linha.setOperacao(operacao);
		linha.setRecursoId(recursoId);
		linha.setValorAntes(cortar(valorAntes));
		linha.setValorDepois(cortar(valorDepois));
		linha.setDetalhe(cortar(detalhe));
		repositorio.save(linha);
	}

	public String json(Object valor) {
		return JsonAuditoria.escrever(valor);
	}

	public static BigDecimal valor(Pedido pedido) {
		BigDecimal total = BigDecimal.ZERO;
		if (pedido.getItens() == null) {
			return total.setScale(2, RoundingMode.HALF_UP);
		}
		for (PedidoItem item : pedido.getItens()) {
			if (item.getPrecoUnitario() == null || item.getQuantidade() == null) {
				continue;
			}
			total = total.add(item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade())));
		}
		return total.setScale(2, RoundingMode.HALF_UP);
	}

	private String cortar(String texto) {
		if (texto == null || texto.length() <= LIMITE) {
			return texto;
		}
		return texto.substring(0, LIMITE);
	}
}
