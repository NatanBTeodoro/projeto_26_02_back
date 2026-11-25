package com.projeto.entity.enums;

/**
 * Função operacional, separada do vínculo ({@link Perfil}) e do restaurante.
 * No token cada valor vira a autoridade {@code FUNCAO_<nome>}.
 * Cargo não entra aqui: o cargo só expande estas authorities.
 */
public enum AuthorityFuncao {
	/** Listar, criar e remover contas de TI. {@code /api/ti/usuarios}. */
	PLATAFORMA_TI_GERIR,
	/** Listar, criar, inativar, reativar e excluir administradores. {@code /api/ti/admins}. */
	PLATAFORMA_ADMIN_GERIR,
	/** Listar a equipe. {@code GET /api/usuarios}. */
	EQUIPE_LER,
	/** Criar, editar e excluir usuário de salão. {@code /api/usuarios} com perfil USUARIO. */
	EQUIPE_USUARIO_GERIR,
	/** Criar, editar e excluir gestor. {@code /api/usuarios} com perfil GESTOR. */
	EQUIPE_GESTOR_GERIR,
	/** Redefinir a senha de outra conta. {@code POST /api/usuarios/{id}/senha}. */
	EQUIPE_SENHA_REDEFINIR,
	/** Conceder ou revogar função ou cargo. {@code POST /api/usuarios/{id}/funcoes}. */
	EQUIPE_FUNCAO_ATRIBUIR,
	/** Ler categoria, produto e prévia da importação. */
	CARDAPIO_LER,
	/** Criar e editar categoria e produto, e confirmar importação. */
	CARDAPIO_ESCREVER,
	/** Ver {@code custoAtual} no cardápio e no DTO de produto. */
	CUSTO_LER,
	/** Listar comandas. O recorte próprio ou da unidade fica no vínculo, não nesta authority. */
	COMANDA_LER,
	/** Abrir comanda. O recorte próprio ou da unidade fica no vínculo, não nesta authority. */
	COMANDA_ABRIR,
	/** Fechar comanda de qualquer usuário do restaurante do contexto. {@code POST /api/comandas/{id}/fechar}. */
	COMANDA_FECHAR,
	/** Listar pedidos. O recorte próprio ou da unidade fica no vínculo, não nesta authority. */
	PEDIDO_LER,
	/** Lançar pedido. {@code POST /api/pedidos}. */
	PEDIDO_LANCAR,
	/** Editar pedido ativo. {@code PUT /api/pedidos/{id}}. */
	PEDIDO_EDITAR,
	/** Cancelar pedido ativo de qualquer usuário do restaurante do contexto. */
	PEDIDO_CANCELAR,
	/** Estornar pedido ainda ativo de qualquer usuário do restaurante do contexto. */
	PEDIDO_ESTORNAR,
	/** Estornar pedido já pago. {@code POST /api/pedidos/{id}/estorno}. */
	PEDIDO_ESTORNAR_PAGO,
	/** Ver contagem, mais vendidos e itens por classe. {@code GET /api/dashboard} e {@code /api/analise}. */
	ANALISE_VER_OPERACIONAL,
	/** Ver faturamento, custo bruto, lucro, margem, ticket e CSV. */
	ANALISE_VER_FINANCEIRO,
	/** Criar e editar o catálogo de funções e de cargos da rede. */
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
