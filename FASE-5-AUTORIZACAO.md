# Fase 5 — catálogo reduzido, cargo e custo bruto

O catálogo atribuível ficou em **26** authorities. Cargo não vira `ROLE_` e o filtro financeiro olha a authority, não o perfil. Custo bruto e cobertura de custo entram nos indicadores financeiros.

## Parte A — redução

Consolidadas, porque eram a mesma decisão fatiada:

| Antes | Depois |
|---|---|
| `COMANDA_LER_PROPRIA` + `COMANDA_LER_UNIDADE` | `COMANDA_LER` |
| `COMANDA_ABRIR_PROPRIA` + `COMANDA_ABRIR_UNIDADE` | `COMANDA_ABRIR` |
| `PEDIDO_LER_PROPRIO` + `PEDIDO_LER_UNIDADE` | `PEDIDO_LER` |
| `PEDIDO_ESTORNAR_ATIVO` | `PEDIDO_ESTORNAR` |

Removidas do catálogo e das tabelas de atribuição, com auditoria `MIGRACAO_REDUCAO`:

- `CONTA_PROPRIA` — `/api/auth/**` continua autenticado.
- `RESTAURANTE_GERIR`, `REDE_VER` — o vínculo de administrador segue no filtro (`hasRole`) e em `exigirAdmin`.
- `EQUIPE_PERFIL_TROCAR` — a troca de perfil ficou só em `TrocaPerfil.validar`, e só o administrador troca. O gestor continua sem essa ação.

Quem lê ou abre comanda/pedido da casa inteira é gestor ou administrador (vínculo). Usuário de salão continua na própria conta. Fechar, cancelar e estornar não usam esse recorte: quem tem a authority age em qualquer pedido do restaurante do contexto.

Pacote padrão:

- TI: `PLATAFORMA_TI_GERIR`, `PLATAFORMA_ADMIN_GERIR`.
- Administrador: 24 authorities de equipe, cardápio, comanda, pedido, análise, reclamação, catálogo e auditoria. Sem plataforma.
- Gestor: o pacote do administrador sem `EQUIPE_GESTOR_GERIR`, `CATALOGO_FUNCOES_GERIR` e `AUDITORIA_VER` (21).
- Usuário: `COMANDA_LER`, `COMANDA_ABRIR`, `PEDIDO_LER`, `PEDIDO_LANCAR`, `CARDAPIO_LER`.

Contas de TI já gravadas podem conservar `RECLAMACAO_LER` e `RECLAMACAO_RESPONDER`. Conta nova de TI não recebe essas duas. O `PUT` de reclamação continua restrito a TI e administrador no filtro.

Testes: `ReducaoCatalogoTest`, `CatalogoFuncaoPadraoTest`.

## Parte B — CargoRede

`CargoRede` (admin, nome, descrição, ativo) e `CargoRedeAuthority`. A linha de função ganhou `cargoOrigemId`: preenchido quando veio de um cargo, nulo quando é avulsa.

Endpoints:

- `GET/POST /api/admin/cargos` e `PUT /api/admin/cargos/{id}` — administrador com `CATALOGO_FUNCOES_GERIR`.
- `GET /api/cargos/atribuiveis` — só cargos cujas authorities o chamador tem todas.
- `POST /api/usuarios/{id}/funcoes` aceita `cargos`, `authorities`, `revogarCargos` e `revogarAuthorities`, além de `conceder`/`revogar`.

Não-escalada: plataforma no cargo responde 403. Gestor não atribui cargo com authority que ele não tem. Alterar as authorities de um cargo já atribuído propaga as linhas e grava `PROPAGACAO_CARGO`.

Cargos criados para cada administrador, na migração e ao cadastrar um administrador novo:

| Cargo | Conteúdo |
|---|---|
| Gerente | pacote do gestor |
| Caixa | ler/abrir/fechar comanda, ler/lançar/editar/cancelar pedido, estornar ativo. Sem estorno de pago |
| Atendente | pacote do usuário |
| Estoquista | ler e escrever cardápio, ler custo |
| Supervisor | caixa + `PEDIDO_ESTORNAR_PAGO` + análise operacional |
| Analista | as duas análises, ler cardápio e custo |

Gestor novo fica ligado ao cargo Gerente. Usuário novo fica ligado ao Atendente. O backfill faz o mesmo nas contas que já existiam.

### Caixa é usuário

As três rotas financeiras passaram de `hasAnyRole("GESTOR","ADMINISTRADOR")` para `hasAnyAuthority`:

- fechar comanda: `FUNCAO_COMANDA_FECHAR`
- estorno: `FUNCAO_PEDIDO_ESTORNAR` ou `FUNCAO_PEDIDO_ESTORNAR_PAGO`
- cancelamento: `FUNCAO_PEDIDO_CANCELAR`

O serviço continua exigindo a authority. Usuário sem essas authorities recebe 403 no filtro. Usuário com o cargo Caixa passa do filtro em fechar, cancelar e estornar ativo. Estorno de pago continua exigindo `PEDIDO_ESTORNAR_PAGO`, que o Caixa não tem. Comanda de outro restaurante segue 404. Gestor sem a authority também recebe 403.

`COMANDA_FECHAR`, `PEDIDO_CANCELAR` e `PEDIDO_ESTORNAR` são de unidade: valem para qualquer usuário do restaurante do contexto.

O fechamento já grava, na auditoria financeira, quem, quando, restaurante, operação, valor antes, valor depois, id da comanda e a lista de pedidos que viraram pagos com o total reconhecido.

A tela de atribuição lista cargos primeiro e, em Avançado, as authorities avulsas. O catálogo de cargos do administrador fica em Funções da rede.

## Parte C — custo bruto

O custo é o gravado no item do pedido, não o custo atual do produto. Entram só itens com custo.

- custo bruto = soma de custo × quantidade
- faturamento com custo = soma de preço × quantidade desses itens
- cobertura = faturamento com custo ÷ faturamento total, em percentual

Exemplo coberto por teste: dois itens, quantidades 2, preços 5,00, um custo 1,50 e o outro sem custo. Faturamento 20,00, faturamento com custo 10,00, custo bruto 3,00, lucro bruto 7,00, cobertura 50%.

Os campos `custoBruto` e `coberturaDeCusto` saem no dashboard, na análise, no painel do administrador, na curva ABC e nas colunas do CSV. Sem `ANALISE_VER_FINANCEIRO` eles vão nulos, no mesmo critério de faturamento, lucro e margem. O CSV omite Custo bruto e Cobertura de custo junto com Popularidade, Lucro unitário e Margem.

## Parte D — teste de arquitetura

`ArquiteturaFinanceiraTest` lê o fonte, sem dependência nova.

- As três rotas financeiras usam `hasAnyAuthority` e não `hasAnyRole`.
- O `SecurityConfig` não cita `CONTA_PROPRIA`, `RESTAURANTE_GERIR`, `REDE_VER` nem `EQUIPE_PERFIL_TROCAR`.
- `fechar`, `estornar` e `cancelar` exigem a authority antes de mudar o pedido.

Uma exceção leva `@SuppressWarnings("arquitetura-financeira")` no método e um comentário com o motivo. O teste não vê um caminho novo que marque pedido como pago sem passar por `FechamentoComanda.aplicar`.

## O que ficou de fora

- Fase 4: teto de valor, quatro olhos e relatório de auditoria financeira.
- Fase 6: restringir gestor em cardápio e indicadores, se isso for objetivo.

## Riscos

- Estorno de pago ainda não tem teto.
- Quem tem a authority de fechar, cancelar ou estornar ativo alcança o pedido de outro usuário da mesma casa. Isso é o desenho do Caixa.
- Conta de TI antiga pode continuar com reclamação no banco; conta nova não recebe.
- O gestor passou a ter `RECLAMACAO_RESPONDER` no pacote, mas o filtro do `PUT` de reclamação continua em TI e administrador.
