package com.projeto.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.projeto.config.SecurityConfig;
import com.projeto.repository.UsuarioRepository;
import com.projeto.seguranca.JsonAuthHandlers;
import com.projeto.seguranca.UsuarioDetailsService;
import com.projeto.service.AnaliseService;
import com.projeto.service.CategoriaService;
import com.projeto.service.ComandaService;
import com.projeto.service.CsvMatriz;
import com.projeto.service.DashboardService;
import com.projeto.service.ImportacaoCardapioService;
import com.projeto.service.PedidoService;
import com.projeto.service.ProdutoService;

@WebMvcTest(controllers = {
		PedidoController.class,
		ComandaController.class,
		CategoriaController.class,
		ProdutoController.class,
		ImportacaoCardapioController.class,
		DashboardController.class,
		AnaliseController.class
})
@AutoConfigureMockMvc
@Import({ SecurityConfig.class, JsonAuthHandlers.class })
class AcessoOperacaoHttpTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PedidoService pedidoService;
	@MockitoBean
	private ComandaService comandaService;
	@MockitoBean
	private CategoriaService categoriaService;
	@MockitoBean
	private ProdutoService produtoService;
	@MockitoBean
	private ImportacaoCardapioService importacaoCardapioService;
	@MockitoBean
	private DashboardService dashboardService;
	@MockitoBean
	private AnaliseService analiseService;
	@MockitoBean
	private CsvMatriz csvMatriz;
	@MockitoBean
	private UsuarioRepository usuarioRepository;
	@MockitoBean
	private UsuarioDetailsService usuarioDetailsService;

	@Test
	@WithMockUser(roles = "USUARIO")
	void usuarioRecebe403NasOperacoesFinanceirasNoFiltro() throws Exception {
		mockMvc.perform(post("/api/comandas/1/fechar")).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/pedidos/1/estorno")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/pedidos/1/cancelamento")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "TI")
	void tiRecebe403NasOperacoesFinanceirasNoFiltro() throws Exception {
		mockMvc.perform(post("/api/comandas/1/fechar")).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/pedidos/1/estorno")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/pedidos/1/cancelamento")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "GESTOR")
	void gestorSemFuncaoRecebe403NasOperacoesFinanceiras() throws Exception {
		mockMvc.perform(post("/api/comandas/1/fechar")).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/pedidos/1/estorno")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/pedidos/1/cancelamento")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"x\"}")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(authorities = {
			"ROLE_GESTOR",
			"FUNCAO_COMANDA_FECHAR",
			"FUNCAO_PEDIDO_ESTORNAR",
			"FUNCAO_PEDIDO_ESTORNAR_PAGO",
			"FUNCAO_PEDIDO_CANCELAR"
	})
	void gestorComFuncaoPassaDoFiltroNasOperacoesFinanceiras() throws Exception {
		mockMvc.perform(post("/api/comandas/1/fechar")).andExpect(status().isOk());
		mockMvc.perform(post("/api/pedidos/1/estorno")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"x\"}")).andExpect(status().isOk());
		mockMvc.perform(post("/api/pedidos/1/cancelamento")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"x\"}")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(authorities = {
			"ROLE_USUARIO",
			"FUNCAO_COMANDA_FECHAR",
			"FUNCAO_PEDIDO_ESTORNAR",
			"FUNCAO_PEDIDO_CANCELAR"
	})
	void usuarioComCargoCaixaPassaDoFiltroFinanceiro() throws Exception {
		mockMvc.perform(post("/api/comandas/1/fechar")).andExpect(status().isOk());
		mockMvc.perform(post("/api/pedidos/1/estorno")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"x\"}")).andExpect(status().isOk());
		mockMvc.perform(post("/api/pedidos/1/cancelamento")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"x\"}")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(authorities = { "ROLE_ADMINISTRADOR", "FUNCAO_PEDIDO_ESTORNAR_PAGO" })
	void administradorPassaDoFiltroNasOperacoesFinanceiras() throws Exception {
		mockMvc.perform(post("/api/pedidos/1/estorno")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"motivo\":\"x\"}")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "USUARIO")
	void usuarioRecebe403AoEscreverCardapioEAoVerIndicador() throws Exception {
		mockMvc.perform(post("/api/categorias")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Bebida\"}")).andExpect(status().isForbidden());
		mockMvc.perform(put("/api/categorias/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Bebida\"}")).andExpect(status().isForbidden());
		mockMvc.perform(post("/api/produtos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"categoriaId\":1,\"nome\":\"Agua\",\"precoAtual\":2}")).andExpect(status().isForbidden());
		mockMvc.perform(put("/api/produtos/1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"categoriaId\":1,\"nome\":\"Agua\",\"precoAtual\":2}")).andExpect(status().isForbidden());
		MockMultipartFile arquivo = new MockMultipartFile("arquivo", "cardapio.csv", "text/csv", "a,b".getBytes());
		mockMvc.perform(multipart("/api/produtos/importar/confirmar").file(arquivo))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/dashboard")).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/analise")).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/analise/exportar")).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "GESTOR")
	void gestorPassaDoFiltroEmCardapioEIndicador() throws Exception {
		mockMvc.perform(post("/api/categorias")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"Bebida\"}")).andExpect(status().isOk());
		mockMvc.perform(get("/api/dashboard")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/analise")).andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/produtos/importar/modelo")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(roles = "USUARIO")
	void usuarioAindaBaixaOModeloDoCardapio() throws Exception {
		mockMvc.perform(get("/api/produtos/importar/modelo")).andExpect(status().isOk());
	}
}
