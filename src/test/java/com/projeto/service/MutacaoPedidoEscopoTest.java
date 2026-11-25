package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.projeto.dto.PedidoDTO;
import com.projeto.entity.Pedido;
import com.projeto.entity.PedidoItem;
import com.projeto.entity.Produto;
import com.projeto.entity.Restaurante;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.AuthorityFuncao;
import com.projeto.entity.enums.OperacaoAuditoria;
import com.projeto.entity.enums.Perfil;
import com.projeto.entity.enums.StatusPedido;
import com.projeto.entity.enums.StatusRestaurante;
import com.projeto.repository.ComandaRepository;
import com.projeto.repository.PedidoRepository;
import com.projeto.repository.ProdutoRepository;
import com.projeto.repository.UsuarioRepository;
import com.projeto.seguranca.EscopoOperacao;
import com.projeto.util.exception.RecursoNaoEncontradoException;
import com.projeto.util.exception.RegraDeNegocioException;

@ExtendWith(MockitoExtension.class)
class MutacaoPedidoEscopoTest {

	@Mock
	private PedidoRepository pedidoRepository;
	@Mock
	private ProdutoRepository produtoRepository;
	@Mock
	private ComandaRepository comandaRepository;
	@Mock
	private ComandaService comandaService;
	@Mock
	private UsuarioRepository usuarioRepository;
	@Mock
	private EscopoOperacao escopo;
	@Mock
	private AuditoriaFinanceiraService auditoria;
	@Mock
	private Autorizacao autorizacao;

	private PedidoService service;
	private Restaurante casaA;
	private Usuario usuarioA;

	@BeforeEach
	void preparar() {
		service = new PedidoService(
				pedidoRepository,
				produtoRepository,
				comandaRepository,
				comandaService,
				usuarioRepository,
				escopo,
				auditoria,
				autorizacao);
		casaA = casa(1L);
		usuarioA = pessoa(10L, Perfil.USUARIO);
		lenient().when(escopo.restauranteDoContexto()).thenReturn(casaA);
		lenient().when(escopo.usuario()).thenReturn(usuarioA);
	}

	@Test
	void pedidoDeOutraCasaVolta404NoEstornoNoCancelamentoENaEdicao() {
		when(pedidoRepository.findById(8L)).thenReturn(Optional.of(pedido(8L, casa(2L), StatusPedido.ATIVO)));

		assertThrows(RecursoNaoEncontradoException.class, () -> service.estornar(8L, new PedidoDTO.Estorno("x")));
		assertThrows(RecursoNaoEncontradoException.class, () -> service.cancelar(8L, new PedidoDTO.Cancelamento("x")));
		assertThrows(RecursoNaoEncontradoException.class, () -> service.editar(8L, null));
		verify(auditoria, never()).registrar(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void pedidoInexistenteVolta404() {
		when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(RecursoNaoEncontradoException.class, () -> service.estornar(99L, new PedidoDTO.Estorno("x")));
	}

	@Test
	void pedidoEncerradoDaPropriaCasaVolta400AntesDoPerfil() {
		when(pedidoRepository.findById(3L)).thenReturn(Optional.of(pedido(3L, casaA, StatusPedido.CANCELADO)));

		RegraDeNegocioException erro = assertThrows(
				RegraDeNegocioException.class,
				() -> service.cancelar(3L, new PedidoDTO.Cancelamento("x")));
		assertEquals("Pedido já encerrado", erro.getMessage());
	}

	@Test
	void pedidoAtivoDaPropriaCasaSemPerfilVolta403() {
		when(pedidoRepository.findById(4L)).thenReturn(Optional.of(pedido(4L, casaA, StatusPedido.ATIVO)));
		doThrow(new AccessDeniedException("Acesso negado")).when(autorizacao).exigir(any());

		assertThrows(AccessDeniedException.class, () -> service.estornar(4L, new PedidoDTO.Estorno("x")));
		assertThrows(AccessDeniedException.class, () -> service.cancelar(4L, new PedidoDTO.Cancelamento("x")));
		assertThrows(AccessDeniedException.class, () -> service.editar(4L, null));
	}

	@Test
	void estornoDePedidoAtivoGeraUmRegistro() {
		usuarioA.setPerfil(Perfil.GESTOR);
		Pedido pedido = pedido(4L, casaA, StatusPedido.ATIVO);
		when(pedidoRepository.findById(4L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(escopo.autor()).thenReturn("Gestor");
		when(auditoria.json(any())).thenReturn("{}");

		service.estornar(4L, new PedidoDTO.Estorno("mesa errada"));

		ArgumentCaptor<OperacaoAuditoria> operacao = ArgumentCaptor.forClass(OperacaoAuditoria.class);
		verify(auditoria).registrar(eq(1L), eq(usuarioA), operacao.capture(), eq(4L), eq("{}"), eq("{}"), eq("mesa errada"));
		assertEquals(OperacaoAuditoria.ESTORNO_ATIVO, operacao.getValue());
	}

	@Test
	void estornoDePedidoPagoGeraEstornoPago() {
		usuarioA.setPerfil(Perfil.GESTOR);
		Pedido pedido = pedido(5L, casaA, StatusPedido.PAGO);
		when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(pedido)).thenReturn(pedido);
		when(escopo.autor()).thenReturn("Gestor");
		when(auditoria.json(any())).thenReturn("{\"status\":\"PAGO\"}");

		service.estornar(5L, new PedidoDTO.Estorno("cobrado a mais"));

		verify(auditoria).registrar(
				eq(1L),
				eq(usuarioA),
				eq(OperacaoAuditoria.ESTORNO_PAGO),
				eq(5L),
				any(),
				any(),
				eq("cobrado a mais"));
	}

	@Test
	void estornoPagoSemAuthorityDePagoVolta403() {
		Pedido pedido = pedido(5L, casaA, StatusPedido.PAGO);
		when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));
		doThrow(new AccessDeniedException("Acesso negado"))
				.when(autorizacao)
				.exigir(AuthorityFuncao.PEDIDO_ESTORNAR_PAGO);

		assertThrows(AccessDeniedException.class, () -> service.estornar(5L, new PedidoDTO.Estorno("x")));
		verify(auditoria, never()).registrar(any(), any(), any(), any(), any(), any(), any());
	}

	private Restaurante casa(Long id) {
		Restaurante restaurante = new Restaurante();
		restaurante.setId(id);
		restaurante.setStatus(StatusRestaurante.ATIVO);
		return restaurante;
	}

	private Usuario pessoa(Long id, Perfil perfil) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setPerfil(perfil);
		usuario.setNome("Pessoa");
		return usuario;
	}

	private Pedido pedido(Long id, Restaurante restaurante, StatusPedido status) {
		Pedido pedido = new Pedido();
		pedido.setId(id);
		pedido.setRestaurante(restaurante);
		pedido.setStatus(status);
		Produto produto = new Produto();
		produto.setId(1L);
		produto.setNome("Agua");
		PedidoItem item = new PedidoItem();
		item.setProduto(produto);
		item.setQuantidade(2);
		item.setPrecoUnitario(new BigDecimal("10.00"));
		pedido.getItens().add(item);
		return pedido;
	}
}
