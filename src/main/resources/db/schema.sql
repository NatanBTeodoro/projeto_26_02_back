CREATE TABLE IF NOT EXISTS usuario (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    senha_hash VARCHAR(72) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    token_acesso VARCHAR(80),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS token_acesso VARCHAR(80);
CREATE UNIQUE INDEX IF NOT EXISTS uk_usuario_token ON usuario (token_acesso);

CREATE TABLE IF NOT EXISTS categoria (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS produto (
    id BIGSERIAL PRIMARY KEY,
    categoria_id BIGINT NOT NULL REFERENCES categoria (id),
    nome VARCHAR(150) NOT NULL,
    preco_atual NUMERIC(12, 2) NOT NULL,
    custo_atual NUMERIC(12, 2),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_produto_preco CHECK (preco_atual >= 0)
);

CREATE TABLE IF NOT EXISTS pedido (
    id BIGSERIAL PRIMARY KEY,
    data_hora TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    motivo_estorno VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS pedido_item (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedido (id),
    produto_id BIGINT NOT NULL REFERENCES produto (id),
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(12, 2) NOT NULL,
    custo_unitario NUMERIC(12, 2),
    CONSTRAINT ck_pedido_item_qtd CHECK (quantidade > 0),
    CONSTRAINT ck_pedido_item_preco CHECK (preco_unitario >= 0)
);

CREATE INDEX IF NOT EXISTS idx_pedido_data ON pedido (data_hora);
CREATE INDEX IF NOT EXISTS idx_pedido_item_pedido ON pedido_item (pedido_id);

CREATE TABLE IF NOT EXISTS restaurante (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    admin_id BIGINT NOT NULL REFERENCES usuario (id),
    status VARCHAR(20) NOT NULL,
    inativado_pelo_admin BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS nome VARCHAR(120);
UPDATE usuario SET nome = split_part(email, '@', 1) WHERE nome IS NULL OR btrim(nome) = '';
ALTER TABLE usuario ALTER COLUMN nome SET NOT NULL;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS restaurante_id BIGINT REFERENCES restaurante (id);
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS inativado_pelo_admin BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS inativado_pelo_restaurante BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS comanda (
    id BIGSERIAL PRIMARY KEY,
    restaurante_id BIGINT NOT NULL REFERENCES restaurante (id),
    usuario_id BIGINT REFERENCES usuario (id),
    numero INTEGER,
    mesa INTEGER,
    nome_cliente VARCHAR(120),
    criada_em TIMESTAMPTZ NOT NULL,
    aberta BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE comanda ADD COLUMN IF NOT EXISTS numero INTEGER;
ALTER TABLE comanda ADD COLUMN IF NOT EXISTS mesa INTEGER;
ALTER TABLE comanda ADD COLUMN IF NOT EXISTS nome_cliente VARCHAR(120);

UPDATE comanda c
SET numero = sub.n
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY restaurante_id ORDER BY id) AS n
    FROM comanda
    WHERE numero IS NULL
) sub
WHERE c.id = sub.id AND c.numero IS NULL;

ALTER TABLE comanda ALTER COLUMN numero SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_comanda_restaurante_numero ON comanda (restaurante_id, numero);

ALTER TABLE pedido ADD COLUMN IF NOT EXISTS restaurante_id BIGINT REFERENCES restaurante (id);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS usuario_id BIGINT REFERENCES usuario (id);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS usuario_nome VARCHAR(120);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS lancado_por VARCHAR(120);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS comanda_id BIGINT REFERENCES comanda (id);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS mesa INTEGER;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS nome_cliente VARCHAR(120);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS motivo_cancelamento VARCHAR(255);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS data_hora_estorno TIMESTAMPTZ;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS data_hora_cancelamento TIMESTAMPTZ;
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS autor_estorno VARCHAR(120);
ALTER TABLE pedido ADD COLUMN IF NOT EXISTS autor_cancelamento VARCHAR(120);

UPDATE comanda c
SET mesa = p.mesa,
    nome_cliente = p.nome_cliente
FROM (
    SELECT DISTINCT ON (comanda_id) comanda_id, mesa, nome_cliente
    FROM pedido
    WHERE comanda_id IS NOT NULL
    ORDER BY comanda_id, data_hora DESC
) p
WHERE c.id = p.comanda_id;

CREATE TABLE IF NOT EXISTS pedido_alteracao (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedido (id),
    data_hora TIMESTAMPTZ NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    autor VARCHAR(120) NOT NULL
);

ALTER TABLE categoria ADD COLUMN IF NOT EXISTS restaurante_id BIGINT REFERENCES restaurante (id);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS restaurante_id BIGINT REFERENCES restaurante (id);

CREATE TABLE IF NOT EXISTS reclamacao (
    id BIGSERIAL PRIMARY KEY,
    autor_id BIGINT REFERENCES usuario (id) ON DELETE SET NULL,
    autor_nome VARCHAR(120) NOT NULL,
    autor_email VARCHAR(255) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL,
    assunto VARCHAR(150) NOT NULL,
    descricao VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    resposta VARCHAR(4000),
    respondido_em TIMESTAMPTZ,
    anexo_nome VARCHAR(255),
    destino VARCHAR(20) NOT NULL DEFAULT 'TI',
    admin_destino_id BIGINT REFERENCES usuario (id) ON DELETE SET NULL,
    restaurante_nome VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS reclamacao_anexo (
    reclamacao_id BIGINT PRIMARY KEY REFERENCES reclamacao (id) ON DELETE CASCADE,
    tipo VARCHAR(120),
    conteudo BYTEA NOT NULL
);

ALTER TABLE reclamacao ADD COLUMN IF NOT EXISTS destino VARCHAR(20) NOT NULL DEFAULT 'TI';
ALTER TABLE reclamacao ADD COLUMN IF NOT EXISTS admin_destino_id BIGINT REFERENCES usuario (id) ON DELETE SET NULL;
ALTER TABLE reclamacao ADD COLUMN IF NOT EXISTS restaurante_nome VARCHAR(150);

CREATE INDEX IF NOT EXISTS idx_reclamacao_autor ON reclamacao (autor_id);
CREATE INDEX IF NOT EXISTS idx_reclamacao_criado ON reclamacao (criado_em);
CREATE INDEX IF NOT EXISTS idx_reclamacao_destino ON reclamacao (destino, admin_destino_id);

CREATE INDEX IF NOT EXISTS idx_restaurante_admin ON restaurante (admin_id);
CREATE INDEX IF NOT EXISTS idx_usuario_restaurante ON usuario (restaurante_id);
CREATE INDEX IF NOT EXISTS idx_pedido_restaurante ON pedido (restaurante_id);
CREATE INDEX IF NOT EXISTS idx_pedido_usuario ON pedido (usuario_id);
CREATE INDEX IF NOT EXISTS idx_pedido_comanda ON pedido (comanda_id);
CREATE INDEX IF NOT EXISTS idx_comanda_restaurante ON comanda (restaurante_id);

CREATE TABLE IF NOT EXISTS auditoria_financeira (
    id BIGSERIAL PRIMARY KEY,
    data_hora TIMESTAMPTZ NOT NULL,
    restaurante_id BIGINT,
    usuario_id BIGINT,
    perfil_no_momento VARCHAR(20),
    operacao VARCHAR(40) NOT NULL,
    recurso_id BIGINT,
    valor_antes VARCHAR(4000),
    valor_depois VARCHAR(4000),
    detalhe VARCHAR(4000)
);

CREATE INDEX IF NOT EXISTS idx_auditoria_financeira_restaurante ON auditoria_financeira (restaurante_id, data_hora);

CREATE TABLE IF NOT EXISTS usuario_funcao_global (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    authority VARCHAR(40) NOT NULL,
    atribuido_por BIGINT,
    atribuido_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_usuario_funcao_global UNIQUE (usuario_id, authority)
);

CREATE TABLE IF NOT EXISTS usuario_restaurante_funcao (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    restaurante_id BIGINT NOT NULL REFERENCES restaurante (id) ON DELETE CASCADE,
    authority VARCHAR(40) NOT NULL,
    atribuido_por BIGINT,
    atribuido_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_usuario_restaurante_funcao UNIQUE (usuario_id, restaurante_id, authority)
);

CREATE TABLE IF NOT EXISTS funcao_rede (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    authority VARCHAR(40) NOT NULL,
    rotulo VARCHAR(80) NOT NULL,
    descricao VARCHAR(400),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_funcao_rede UNIQUE (admin_id, authority)
);

CREATE TABLE IF NOT EXISTS auditoria_permissao (
    id BIGSERIAL PRIMARY KEY,
    data_hora TIMESTAMPTZ NOT NULL,
    autor_id BIGINT,
    alvo_id BIGINT NOT NULL,
    restaurante_id BIGINT,
    authority VARCHAR(40) NOT NULL,
    acao VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_auditoria_permissao_alvo ON auditoria_permissao (alvo_id, data_hora);

-- Fase 5A: consolida o par próprio/unidade e renomeia o estorno de pedido ativo.
DELETE FROM usuario_restaurante_funcao a
USING usuario_restaurante_funcao b
WHERE a.usuario_id = b.usuario_id
  AND a.restaurante_id = b.restaurante_id
  AND a.id <> b.id
  AND (
    (a.authority = 'COMANDA_LER_PROPRIA' AND b.authority = 'COMANDA_LER_UNIDADE')
    OR (a.authority = 'COMANDA_ABRIR_PROPRIA' AND b.authority = 'COMANDA_ABRIR_UNIDADE')
    OR (a.authority = 'PEDIDO_LER_PROPRIO' AND b.authority = 'PEDIDO_LER_UNIDADE')
  );

UPDATE usuario_restaurante_funcao a
SET authority = 'COMANDA_LER'
WHERE a.authority IN ('COMANDA_LER_PROPRIA', 'COMANDA_LER_UNIDADE')
AND NOT EXISTS (
    SELECT 1 FROM usuario_restaurante_funcao b
    WHERE b.usuario_id = a.usuario_id AND b.restaurante_id = a.restaurante_id AND b.authority = 'COMANDA_LER'
);
UPDATE usuario_restaurante_funcao a
SET authority = 'COMANDA_ABRIR'
WHERE a.authority IN ('COMANDA_ABRIR_PROPRIA', 'COMANDA_ABRIR_UNIDADE')
AND NOT EXISTS (
    SELECT 1 FROM usuario_restaurante_funcao b
    WHERE b.usuario_id = a.usuario_id AND b.restaurante_id = a.restaurante_id AND b.authority = 'COMANDA_ABRIR'
);
UPDATE usuario_restaurante_funcao a
SET authority = 'PEDIDO_LER'
WHERE a.authority IN ('PEDIDO_LER_PROPRIO', 'PEDIDO_LER_UNIDADE')
AND NOT EXISTS (
    SELECT 1 FROM usuario_restaurante_funcao b
    WHERE b.usuario_id = a.usuario_id AND b.restaurante_id = a.restaurante_id AND b.authority = 'PEDIDO_LER'
);
UPDATE usuario_restaurante_funcao SET authority = 'PEDIDO_ESTORNAR' WHERE authority = 'PEDIDO_ESTORNAR_ATIVO';
DELETE FROM usuario_restaurante_funcao
WHERE authority IN ('COMANDA_LER_PROPRIA', 'COMANDA_LER_UNIDADE', 'COMANDA_ABRIR_PROPRIA', 'COMANDA_ABRIR_UNIDADE', 'PEDIDO_LER_PROPRIO', 'PEDIDO_LER_UNIDADE');

DELETE FROM usuario_funcao_global a
USING usuario_funcao_global b
WHERE a.usuario_id = b.usuario_id
  AND a.id <> b.id
  AND (
    (a.authority = 'COMANDA_LER_PROPRIA' AND b.authority = 'COMANDA_LER_UNIDADE')
    OR (a.authority = 'COMANDA_ABRIR_PROPRIA' AND b.authority = 'COMANDA_ABRIR_UNIDADE')
    OR (a.authority = 'PEDIDO_LER_PROPRIO' AND b.authority = 'PEDIDO_LER_UNIDADE')
  );
UPDATE usuario_funcao_global a
SET authority = 'COMANDA_LER'
WHERE a.authority IN ('COMANDA_LER_PROPRIA', 'COMANDA_LER_UNIDADE')
AND NOT EXISTS (
    SELECT 1 FROM usuario_funcao_global b
    WHERE b.usuario_id = a.usuario_id AND b.authority = 'COMANDA_LER'
);
UPDATE usuario_funcao_global a
SET authority = 'COMANDA_ABRIR'
WHERE a.authority IN ('COMANDA_ABRIR_PROPRIA', 'COMANDA_ABRIR_UNIDADE')
AND NOT EXISTS (
    SELECT 1 FROM usuario_funcao_global b
    WHERE b.usuario_id = a.usuario_id AND b.authority = 'COMANDA_ABRIR'
);
UPDATE usuario_funcao_global a
SET authority = 'PEDIDO_LER'
WHERE a.authority IN ('PEDIDO_LER_PROPRIO', 'PEDIDO_LER_UNIDADE')
AND NOT EXISTS (
    SELECT 1 FROM usuario_funcao_global b
    WHERE b.usuario_id = a.usuario_id AND b.authority = 'PEDIDO_LER'
);
UPDATE usuario_funcao_global SET authority = 'PEDIDO_ESTORNAR' WHERE authority = 'PEDIDO_ESTORNAR_ATIVO';
DELETE FROM usuario_funcao_global
WHERE authority IN ('COMANDA_LER_PROPRIA', 'COMANDA_LER_UNIDADE', 'COMANDA_ABRIR_PROPRIA', 'COMANDA_ABRIR_UNIDADE', 'PEDIDO_LER_PROPRIO', 'PEDIDO_LER_UNIDADE');

DELETE FROM funcao_rede a
USING funcao_rede b
WHERE a.admin_id = b.admin_id
  AND a.id <> b.id
  AND (
    (a.authority = 'COMANDA_LER_PROPRIA' AND b.authority = 'COMANDA_LER_UNIDADE')
    OR (a.authority = 'COMANDA_ABRIR_PROPRIA' AND b.authority = 'COMANDA_ABRIR_UNIDADE')
    OR (a.authority = 'PEDIDO_LER_PROPRIO' AND b.authority = 'PEDIDO_LER_UNIDADE')
  );
UPDATE funcao_rede a SET authority = 'COMANDA_LER'
WHERE a.authority IN ('COMANDA_LER_PROPRIA', 'COMANDA_LER_UNIDADE')
AND NOT EXISTS (SELECT 1 FROM funcao_rede b WHERE b.admin_id = a.admin_id AND b.authority = 'COMANDA_LER');
UPDATE funcao_rede a SET authority = 'COMANDA_ABRIR'
WHERE a.authority IN ('COMANDA_ABRIR_PROPRIA', 'COMANDA_ABRIR_UNIDADE')
AND NOT EXISTS (SELECT 1 FROM funcao_rede b WHERE b.admin_id = a.admin_id AND b.authority = 'COMANDA_ABRIR');
UPDATE funcao_rede a SET authority = 'PEDIDO_LER'
WHERE a.authority IN ('PEDIDO_LER_PROPRIO', 'PEDIDO_LER_UNIDADE')
AND NOT EXISTS (SELECT 1 FROM funcao_rede b WHERE b.admin_id = a.admin_id AND b.authority = 'PEDIDO_LER');
UPDATE funcao_rede SET authority = 'PEDIDO_ESTORNAR' WHERE authority = 'PEDIDO_ESTORNAR_ATIVO';
DELETE FROM funcao_rede
WHERE authority IN ('COMANDA_LER_PROPRIA', 'COMANDA_LER_UNIDADE', 'COMANDA_ABRIR_PROPRIA', 'COMANDA_ABRIR_UNIDADE', 'PEDIDO_LER_PROPRIO', 'PEDIDO_LER_UNIDADE');

INSERT INTO auditoria_permissao (data_hora, autor_id, alvo_id, restaurante_id, authority, acao)
SELECT CURRENT_TIMESTAMP, NULL, f.usuario_id, f.restaurante_id, f.authority, 'MIGRACAO_REDUCAO'
FROM usuario_restaurante_funcao f
WHERE f.authority IN ('CONTA_PROPRIA', 'RESTAURANTE_GERIR', 'REDE_VER', 'EQUIPE_PERFIL_TROCAR')
AND NOT EXISTS (
    SELECT 1 FROM auditoria_permissao ap
    WHERE ap.alvo_id = f.usuario_id
      AND ap.authority = f.authority
      AND ap.acao = 'MIGRACAO_REDUCAO'
      AND ap.restaurante_id IS NOT DISTINCT FROM f.restaurante_id
);

INSERT INTO auditoria_permissao (data_hora, autor_id, alvo_id, restaurante_id, authority, acao)
SELECT CURRENT_TIMESTAMP, NULL, f.usuario_id, NULL, f.authority, 'MIGRACAO_REDUCAO'
FROM usuario_funcao_global f
WHERE f.authority IN ('CONTA_PROPRIA', 'RESTAURANTE_GERIR', 'REDE_VER', 'EQUIPE_PERFIL_TROCAR')
AND NOT EXISTS (
    SELECT 1 FROM auditoria_permissao ap
    WHERE ap.alvo_id = f.usuario_id
      AND ap.authority = f.authority
      AND ap.acao = 'MIGRACAO_REDUCAO'
      AND ap.restaurante_id IS NULL
);

DELETE FROM usuario_restaurante_funcao
WHERE authority IN ('CONTA_PROPRIA', 'RESTAURANTE_GERIR', 'REDE_VER', 'EQUIPE_PERFIL_TROCAR');
DELETE FROM usuario_funcao_global
WHERE authority IN ('CONTA_PROPRIA', 'RESTAURANTE_GERIR', 'REDE_VER', 'EQUIPE_PERFIL_TROCAR');
DELETE FROM funcao_rede
WHERE authority IN ('CONTA_PROPRIA', 'RESTAURANTE_GERIR', 'REDE_VER', 'EQUIPE_PERFIL_TROCAR');

INSERT INTO usuario_funcao_global (usuario_id, authority, atribuido_em)
SELECT u.id, f.authority, CURRENT_TIMESTAMP
FROM usuario u
JOIN (VALUES
    ('PLATAFORMA_TI_GERIR'),
    ('PLATAFORMA_ADMIN_GERIR')
) AS f(authority) ON TRUE
WHERE u.perfil = 'TI'
AND NOT EXISTS (
    SELECT 1 FROM usuario_funcao_global g WHERE g.usuario_id = u.id AND g.authority = f.authority
);

INSERT INTO usuario_funcao_global (usuario_id, authority, atribuido_em)
SELECT u.id, f.authority, CURRENT_TIMESTAMP
FROM usuario u
JOIN (VALUES
    ('EQUIPE_LER'),
    ('EQUIPE_USUARIO_GERIR'),
    ('EQUIPE_GESTOR_GERIR'),
    ('EQUIPE_SENHA_REDEFINIR'),
    ('EQUIPE_FUNCAO_ATRIBUIR'),
    ('CATALOGO_FUNCOES_GERIR'),
    ('CARDAPIO_LER'),
    ('CARDAPIO_ESCREVER'),
    ('CUSTO_LER'),
    ('COMANDA_LER'),
    ('COMANDA_ABRIR'),
    ('COMANDA_FECHAR'),
    ('PEDIDO_LER'),
    ('PEDIDO_LANCAR'),
    ('PEDIDO_EDITAR'),
    ('PEDIDO_CANCELAR'),
    ('PEDIDO_ESTORNAR'),
    ('PEDIDO_ESTORNAR_PAGO'),
    ('ANALISE_VER_OPERACIONAL'),
    ('ANALISE_VER_FINANCEIRO'),
    ('RECLAMACAO_ABRIR'),
    ('RECLAMACAO_LER'),
    ('RECLAMACAO_RESPONDER'),
    ('AUDITORIA_VER')
) AS f(authority) ON TRUE
WHERE u.perfil = 'ADMINISTRADOR'
AND NOT EXISTS (
    SELECT 1 FROM usuario_funcao_global g WHERE g.usuario_id = u.id AND g.authority = f.authority
);

INSERT INTO usuario_restaurante_funcao (usuario_id, restaurante_id, authority, atribuido_em)
SELECT u.id, u.restaurante_id, f.authority, CURRENT_TIMESTAMP
FROM usuario u
JOIN (VALUES
    ('EQUIPE_LER'),
    ('EQUIPE_USUARIO_GERIR'),
    ('EQUIPE_SENHA_REDEFINIR'),
    ('EQUIPE_FUNCAO_ATRIBUIR'),
    ('CARDAPIO_LER'),
    ('CARDAPIO_ESCREVER'),
    ('CUSTO_LER'),
    ('COMANDA_LER'),
    ('COMANDA_ABRIR'),
    ('COMANDA_FECHAR'),
    ('PEDIDO_LER'),
    ('PEDIDO_LANCAR'),
    ('PEDIDO_EDITAR'),
    ('PEDIDO_CANCELAR'),
    ('PEDIDO_ESTORNAR'),
    ('PEDIDO_ESTORNAR_PAGO'),
    ('ANALISE_VER_OPERACIONAL'),
    ('ANALISE_VER_FINANCEIRO'),
    ('RECLAMACAO_ABRIR'),
    ('RECLAMACAO_LER'),
    ('RECLAMACAO_RESPONDER')
) AS f(authority) ON TRUE
WHERE u.perfil = 'GESTOR' AND u.restaurante_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM usuario_restaurante_funcao g
    WHERE g.usuario_id = u.id AND g.restaurante_id = u.restaurante_id AND g.authority = f.authority
);

INSERT INTO usuario_restaurante_funcao (usuario_id, restaurante_id, authority, atribuido_em)
SELECT u.id, u.restaurante_id, f.authority, CURRENT_TIMESTAMP
FROM usuario u
JOIN (VALUES
    ('COMANDA_LER'),
    ('COMANDA_ABRIR'),
    ('PEDIDO_LER'),
    ('PEDIDO_LANCAR'),
    ('CARDAPIO_LER')
) AS f(authority) ON TRUE
WHERE u.perfil = 'USUARIO' AND u.restaurante_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM usuario_restaurante_funcao g
    WHERE g.usuario_id = u.id AND g.restaurante_id = u.restaurante_id AND g.authority = f.authority
);

CREATE TABLE IF NOT EXISTS cargo_rede (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    nome VARCHAR(80) NOT NULL,
    descricao VARCHAR(400),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_cargo_rede UNIQUE (admin_id, nome)
);

CREATE TABLE IF NOT EXISTS cargo_rede_authority (
    id BIGSERIAL PRIMARY KEY,
    cargo_rede_id BIGINT NOT NULL REFERENCES cargo_rede (id) ON DELETE CASCADE,
    authority VARCHAR(40) NOT NULL,
    CONSTRAINT uk_cargo_rede_authority UNIQUE (cargo_rede_id, authority)
);

ALTER TABLE usuario_restaurante_funcao ADD COLUMN IF NOT EXISTS cargo_origem_id BIGINT;
ALTER TABLE usuario_funcao_global ADD COLUMN IF NOT EXISTS cargo_origem_id BIGINT;
