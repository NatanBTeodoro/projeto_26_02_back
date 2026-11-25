package com.projeto.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.projeto.entity.AuditoriaFinanceira;
import com.projeto.entity.Usuario;
import com.projeto.entity.enums.OperacaoAuditoria;
import com.projeto.entity.enums.Perfil;
import com.projeto.repository.AuditoriaFinanceiraRepository;

@ExtendWith(MockitoExtension.class)
class AuditoriaFinanceiraServiceTest {

	@Mock
	private AuditoriaFinanceiraRepository repositorio;

	@Test
	void registrarPreencheOsCampos() {
		when(repositorio.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
		AuditoriaFinanceiraService service = new AuditoriaFinanceiraService(repositorio, null);
		Usuario usuario = new Usuario();
		usuario.setId(7L);
		usuario.setPerfil(Perfil.GESTOR);

		service.registrar(3L, usuario, OperacaoAuditoria.CANCELAMENTO, 11L, "antes", "depois", "motivo");

		ArgumentCaptor<AuditoriaFinanceira> captor = ArgumentCaptor.forClass(AuditoriaFinanceira.class);
		verify(repositorio).save(captor.capture());
		AuditoriaFinanceira linha = captor.getValue();
		assertEquals(3L, linha.getRestauranteId());
		assertEquals(7L, linha.getUsuarioId());
		assertEquals("GESTOR", linha.getPerfilNoMomento());
		assertEquals(OperacaoAuditoria.CANCELAMENTO, linha.getOperacao());
		assertEquals(11L, linha.getRecursoId());
		assertEquals("antes", linha.getValorAntes());
		assertEquals("depois", linha.getValorDepois());
		assertEquals("motivo", linha.getDetalhe());
	}

	@Test
	void falhaNaGravacaoNaoPropaga() {
		AuditoriaFinanceiraService service = new AuditoriaFinanceiraService(repositorio, null) {
			@Override
			public void gravar(
					Long restauranteId,
					Usuario usuario,
					OperacaoAuditoria operacao,
					Long recursoId,
					String valorAntes,
					String valorDepois,
					String detalhe) {
				throw new IllegalStateException("banco fora");
			}
		};

		assertDoesNotThrow(() -> service.registrar(
				1L, new Usuario(), OperacaoAuditoria.PRODUTO_CRIADO, 1L, null, "{}", null));
	}
}
