package com.projeto.entity.enums;

/**
 * Função operacional, separada do vínculo ({@link Perfil}) e do restaurante.
 * No token cada valor vira a autoridade {@code FUNCAO_<nome>}.
 */
public enum AuthorityFuncao {
	/** Ver o próprio cadastro, sair e trocar a própria senha. {@code /api/auth/eu}, {@code /logout}, {@code /senha}. */
	CONTA_PROPRIA,
	/** Listar, criar e remover contas de TI. {@code /api/ti/usuarios}. */
	PLATAFORMA_TI_GERIR,
	/** Listar, criar, inativar, reativar e excluir administradores. {@code /api/ti/admins}. */
	PLATAFORMA_ADMIN_GERIR,
	/** Criar e alterar restaurantes da rede. {@code /api/restaurantes}. */
	RESTAURANTE_GERIR,
	/** Listar a equipe. {@code GET /api/usuarios}. */
	EQUIPE_LER,
	/** Criar, editar e excluir usuário de salão. {@code /api/usuarios} com perfil USUARIO. */
	EQUIPE_USUARIO_GERIR,
	/** Criar, editar e excluir gestor. {@code /api/usuarios} com perfil GESTOR. */
	EQUIPE_GESTOR_GERIR,
	/** Trocar o perfil do alvo entre gestor e usuário. {@code PUT /api/usuarios/{id}}. */
	EQUIPE_PERFIL_TROCAR,
	/** Redefinir a senha de outra conta. {@code POST /api/usuarios/{id}/senha}. */
	EQUIPE_SENHA_REDEFINIR,
	/** Conceder ou revogar função. {@code POST /api/usuarios/{id}/funcoes}. */
	EQUIPE_FUNCAO_ATRIBUIR,
	/** Ler categoria, produto, modelo CSV e prévia da importação. */
	CARDAPIO_LER,
	/** Criar e editar categoria e produto, e confirmar importação. */
	CARDAPIO_ESCREVER,
	/** Ver {@code custoAtual} no cardápio e no DTO de produto. */
	CUSTO_LER,
	/** Listar as comandas da própria conta. {@code GET /api/comandas}. */
	COMANDA_LER_PROPRIA,
	/** Listar comandas de um usuário da unidade. {@code GET /api/comandas?usuarioId}. */
	COMANDA_LER_UNIDADE,
	/** Abrir comanda no próprio nome. {@code POST /api/comandas}. */
	COMANDA_ABRIR_PROPRIA,
	/** Abrir comanda para outro usuário da unidade. {@code POST /api/comandas}. */
	COMANDA_ABRIR_UNIDADE,
	/** Fechar comanda e marcar pedidos ativos como pagos. {@code POST /api/comandas/{id}/fechar}. */
	COMANDA_FECHAR,
	/** Listar os próprios pedidos. {@code GET /api/pedidos}. */
	PEDIDO_LER_PROPRIO,
	/** Listar os pedidos da unidade. {@code GET /api/pedidos}. */
	PEDIDO_LER_UNIDADE,
	/** Lançar pedido. {@code POST /api/pedidos}. */
	PEDIDO_LANCAR,
	/** Editar pedido ativo. {@code PUT /api/pedidos/{id}}. */
	PEDIDO_EDITAR,
	/** Cancelar pedido ativo. {@code POST /api/pedidos/{id}/cancelamento}. */
	PEDIDO_CANCELAR,
	/** Estornar pedido ainda ativo. {@code POST /api/pedidos/{id}/estorno}. */
	PEDIDO_ESTORNAR_ATIVO,
	/** Estornar pedido já pago. {@code POST /api/pedidos/{id}/estorno}. */
	PEDIDO_ESTORNAR_PAGO,
	/** Ver contagem, mais vendidos e itens por classe. {@code GET /api/dashboard} e {@code /api/analise}. */
	ANALISE_VER_OPERACIONAL,
	/** Ver faturamento, lucro, margem, ticket e CSV. {@code GET /api/dashboard}, {@code /api/analise}, {@code /exportar}. */
	ANALISE_VER_FINANCEIRO,
	/** Ver painel da rede e curva ABC. {@code /api/painel-admin}, {@code /api/curva-abc}. */
	REDE_VER,
	/** Criar e editar o catálogo de funções da rede. {@code /api/admin/funcoes}. */
	CATALOGO_FUNCOES_GERIR,
	/** Abrir reclamação. {@code POST /api/reclamacoes}. */
	RECLAMACAO_ABRIR,
	/** Listar reclamações e baixar anexo. {@code GET /api/reclamacoes}. */
	RECLAMACAO_LER,
	/** Responder reclamação endereçada a quem chama. {@code PUT /api/reclamacoes/{id}}. */
	RECLAMACAO_RESPONDER,
	/** Consultar auditoria de permissão. {@code GET /api/auditoria/permissoes}. */
	AUDITORIA_VER
}
